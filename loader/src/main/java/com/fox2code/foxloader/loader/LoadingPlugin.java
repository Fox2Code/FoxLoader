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

import com.fox2code.foxloader.loader.java.JavaModInfo;
import com.fox2code.foxloader.updater.AbstractUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public abstract class LoadingPlugin {
    public static final int DISPLAY_FLAG_LIBRARY      = 0x01;
    public static final int DISPLAY_FLAG_GAME_CONTENT = 0x02;
    public static final int DISPLAY_FLAG_MIXIN        = 0x04;
    private final String id;
    JavaModInfo javaModInfo;

    protected LoadingPlugin(String id) {
        this.id = id;
    }

    public final String getPluginId() {
        return this.id;
    }

    public final String getPluginVersion() {
        return this.javaModInfo != null ? this.javaModInfo.version : null;
    }

    public final @Nullable ModInfo getModInfoHelper(@NotNull File mod, @Nullable String jarPath) {
        try {
            return this.getModInfo(mod, jarPath);
        } catch (Exception e) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                    "LoadingPlugin failed to parse mod file " + mod.getPath(), e);
            return null;
        }
    }

    public final @NotNull List<ModInfo> getInjectedModInfoHelper() {
        List<ModInfo> modInfos = null;
        try {
            modInfos = this.getInjectedModInfo();
        } catch (Exception e) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                    "LoadingPlugin failed to inject mods", e);
        }
        return modInfos == null ? Collections.emptyList() : modInfos;
    }

    /**
     * @param mod the mod file
     * @param jarPath the jar path if the mod to load is in a sub-path of the loader.
     * @return a {@link ModInfo} if mod was defined as a potential mod candidate
     * @throws Exception if parsing the mod info failed
     * */
    public abstract @Nullable ModInfo getModInfo(@NotNull File mod, @Nullable String jarPath) throws Exception;

    /**
     * @return a list of {@link ModInfo} to inject, mod info present in the mod folder will be ignored.
     * @throws Exception if loading the mod list to inject failed
     * */
    public @NotNull List<ModInfo> getInjectedModInfo() throws Exception {
        return Collections.emptyList();
    }

    /**
     * Called when the mod container list is finalized and will no longer change,
     * and before any calls to any {@link #preLoadModContainer(ModContainer)}
     */
    public void onModContainerListFinalized() {}

    /**
     * Preload mod container, for example load Mixins and other patch elements
     *
     * @param modContainer the mod container
     * @throws Exception if preloading the mod failed
     * */
    public abstract void preLoadModContainer(@NotNull ModContainer modContainer) throws Exception;

    /**
     * Called after all loaded mod containers have been preloaded!
     */
    public void onAllModContainersPreloaded() {}

    /**
     * Called after the game can be loaded but before mods are initialized.
     */
    public void preModInitialization() {}

    /**
     * Return a {@link Mod} if a mod object was found
     * @param modContainer the mod container
     * @return the mod to be used as the main {@link Mod} instance, may be {@code null}
     * @throws Exception if loading the mod failed
     * */
    public abstract @Nullable Mod loadModContainer(@NotNull ModContainer modContainer) throws Exception;

    /**
     * @param modContainer the mod container
     * @return the display flags to use for the current mod.
     */
    public int getModDisplayFlags(ModContainer modContainer) {
        return 0;
    }

    /**
     * @param modContainer the mod to make an update for
     * @return the update to use for that mod or {@code null} if not applicable.
     */
    public @Nullable AbstractUpdater makeModContainerUpdater(ModContainer modContainer) {
        return null;
    }
}
