/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.loader;

import com.fox2code.foxevents.Event;
import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxloader.config.ConfigIO;
import com.fox2code.foxloader.updater.AbstractUpdater;
import com.fox2code.foxloader.updater.UpdateManager;
import com.fox2code.foxloader.utils.async.FastThreadLocal;
import net.minecraft.common.networking.NetworkManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Level;

import java.util.logging.Logger;

public final class ModContainer {
    private static final FastThreadLocal<ModContainer> activeModContainer = new FastThreadLocal<>();
    // Don't allow some display flags to be set directly by loading plugins
    private static final int DISPLAY_FLAGS_PRIVILEGED = (LoadingPlugin.DISPLAY_FLAG_DISABLED);
    // tmp is used to make getModContainer work in constructor.
    static ModContainer tmp;
    private final LoadingPlugin loadingPlugin;
    private final ModInfo modInfo;
    private final Logger logger;
    private final org.slf4j.Logger slf4jLogger;
    private final String modId;
    private int modDisplayFlags;
    private boolean modDisabled;
    private Object configObject;
    Mod mod;

    ModContainer(LoadingPlugin loadingPlugin, ModInfo modInfo) {
        this.loadingPlugin = loadingPlugin;
        this.modInfo = modInfo;
        this.logger = Logger.getLogger(modInfo.name);
        this.slf4jLogger = org.slf4j.LoggerFactory.getLogger(modInfo.name);
        this.modId = modInfo.id;
        this.modDisplayFlags = this.loadingPlugin.getModDisplayFlags(this) & ~DISPLAY_FLAGS_PRIVILEGED;
    }

    private ModContainer markActive() {
        ModContainer modContainer = activeModContainer.get();
        activeModContainer.set(this);
        return modContainer;
    }

    @Nullable public static ModContainer getActiveModContainer() {
        return activeModContainer.get();
    }

    static void setActiveModContainer(@Nullable ModContainer modContainer)  {
        if (modContainer == null) activeModContainer.remove();
        else activeModContainer.set(modContainer);
    }

    public void runInContext(@NotNull Runnable runnable) {
        ModContainer modContainer = markActive();
        try {
            runnable.run();
        } finally {
            setActiveModContainer(modContainer);
        }
    }

    public <T, R> R runFuncInContext(T thing,@NotNull Function<T, R> func) {
        ModContainer modContainer = markActive();
        R result;
        try {
            result = func.apply(thing);
        } finally {
            setActiveModContainer(modContainer);
        }
        return result;
    }

    @NotNull public ModInfo getModInfo() {
        return this.modInfo;
    }

    @NotNull public String getModId() {
        return this.modId;
    }

    @NotNull public String getModName() {
        return this.modInfo.name;
    }

    @NotNull public Logger getLogger() {
        return this.logger;
    }

    @NotNull public org.slf4j.Logger getSlf4jLogger() {
        return this.slf4jLogger;
    }

    @NotNull public Mod getMod() {
        return this.mod;
    }

    @NotNull public String getFileName() {
        return this.modInfo.fileName;
    }

    void setConfigObject(@Nullable Object configObject) {
        this.configObject = configObject;
        ConfigIO.readAndUpdateConfiguration(this, configObject);
    }

    @Nullable public Object getConfigObject() {
        return this.configObject;
    }

    void preLoadContainer() throws Exception {
        if (!this.isModDisabled()) {
            this.loadingPlugin.preLoadModContainer(this);
        }
    }

    public void onReceiveDataFromClient(@NotNull NetworkManager connection, byte @NotNull [] data) {
        if (this.mod != null) {
            try {
                this.mod.onReceiveDataFromClient(connection, data);
            } catch (Exception e) {
                this.getLogger().log(Level.WARNING, "Failed to read client data", e);
            }
        }
    }

    public void onReceiveDataFromServer(@NotNull NetworkManager connection, byte @NotNull [] data) throws IOException {
        if (this.mod != null) {
            try {
                this.mod.onReceiveDataFromServer(connection, data);
            } catch (Exception e) {
                this.getLogger().log(Level.WARNING, "Failed to read server data", e);
            }
        }
    }

    public void onEventError(@NotNull Event event,@NotNull EventCallback callback,@NotNull Throwable throwable, boolean disable) {
        if (this.mod != null) {
            try {
                this.mod.onEventError(event, callback, throwable, disable);
            } catch (Exception e) {
                this.getLogger().log(Level.WARNING, "Failed to handle onEventError", e);
            }
        }
    }

    public int getModDisplayFlags() {
        return this.modDisplayFlags;
    }

    Mod initializeMod() throws Exception {
        if (this == ModLoaderInit.FOX_LOADER_CONTAINER) {
            return this.mod = new ModLoader();
        }
        if (this.isModDisabled()) {
            return null;
        }
        Mod newMod;
        try {
            tmp = this;
            newMod = this.loadingPlugin.loadModContainer(this);
            if (newMod != null) {
                newMod.modContainer = this;
                this.mod = newMod;
            }
        } finally {
            tmp = null;
        }
        return newMod;
    }

    public void markAddGameContent() {
        if (this == ModLoaderInit.FOX_LOADER_CONTAINER) {
            throw new IllegalStateException("Cannot mark FoxLoader itself as adding game content!");
        }
        this.modDisplayFlags |= LoadingPlugin.DISPLAY_FLAG_GAME_CONTENT;
    }

    void markAddMixin() {
        this.modDisplayFlags |= LoadingPlugin.DISPLAY_FLAG_MIXIN;
    }

    private AbstractUpdater makeModContainerUpdater() {
        return this.isModDisabled() ? null : this.loadingPlugin.makeModContainerUpdater(this);
    }

    public boolean isModDisabled() {
        return this.modDisabled;
    }

    void markModDisabled() {
        this.modDisabled = true;
        this.modDisplayFlags |= LoadingPlugin.DISPLAY_FLAG_DISABLED;
    }

    static void initializeUpdateManager() {
        UpdateManager.getInstance().registerProvider(ModContainer::makeModContainerUpdater);
    }
}
