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

import com.fox2code.foxevents.EventHandler;
import com.fox2code.foxloader.client.gui.GuiButtonCallback;
import com.fox2code.foxloader.client.gui.GuiConfigProvider;
import com.fox2code.foxloader.client.gui.GuiModMenu;
import com.fox2code.foxloader.energy.FoxPowerUtils;
import com.fox2code.foxloader.event.FoxLoaderEvents;
import com.fox2code.foxloader.event.client.GuiScreenInitEvent;
import com.fox2code.foxloader.event.lifecycle.LifecycleStartEvent;
import com.fox2code.foxloader.internal.InternalTranslateHooks;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.packet.LoaderNetworkManager;
import com.fox2code.foxloader.network.SidedMetadataAPI;
import com.fox2code.foxloader.registry.CommandRegistry;
import com.fox2code.foxloader.registry.GameRegistry;
import com.fox2code.foxloader.updater.UpdateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.common.CoreConstants;
import net.minecraft.common.command.Command;
import net.minecraft.common.command.ICommandListener;
import net.minecraft.common.networking.NetworkManager;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class ModLoader extends Mod {
    private static final ArrayList<Mod> mods = new ArrayList<>();
    private static boolean areAllModsLoaded = false;
    private static Thread gameThread;

    static {
        if (!FoxLauncher.getFoxClassLoader().isAllowLoadingGame()) {
            throw new IllegalStateException("Loaded ModLoader too soon, please use ModLoaderInit instead");
        }
    }

    ModLoader() { this.modContainer = ModLoaderInit.FOX_LOADER_CONTAINER; }

    @NotNull public static File getModsFolder() {
        return ModLoaderInit.mods;
    }

    @NotNull public static File getConfigFolder() {
        return ModLoaderInit.config;
    }

    public static Mod getMod(String modId) {
        ModContainer modContainer = ModLoaderInit.modContainers.get(modId);
        return modContainer != null ? modContainer.getMod() : null;
    }

    static void preInitializeMods(Collection<LoadingPlugin> loadingPlugins) throws Exception {
        if (ModLoader.areAllModsLoaded())
            throw new IllegalStateException("Mods are already loaded!");
        if (ModContainer.getActiveModContainer() != null)
            throw new IllegalStateException("preInitializeMods() called with active mod container");
        GameRegistry.initialize();
        if (FoxLauncher.isClient()) {
            ModLoaderInit.getModContainer("reindev").setConfigObject(
                    (GuiConfigProvider) screen ->
                            new GuiOptions(screen, Minecraft.getInstance().gameSettings));
        }
        for (LoadingPlugin loadingPlugin : loadingPlugins) {
            loadingPlugin.preModInitialization();
        }
        for (ModContainer modContainer : ModLoaderInit.modContainers.values()) {
            ModContainer.setActiveModContainer(modContainer);
            Mod mod = modContainer.initializeMod();
            if (mod != null) {
                mods.add(mod);
            }
        }
        for (Mod mod : mods) {
            ModContainer.setActiveModContainer(mod.getModContainer());
            FoxLoaderEvents.INSTANCE.registerEvents(mod);
        }
        for (Mod mod : mods) {
            ModContainer.setActiveModContainer(mod.getModContainer());
            mod.onPreInit();
        }
        ModContainer.setActiveModContainer(null);
        areAllModsLoaded = true;
        GameRegistry.freeze();
        InternalTranslateHooks.notifyInit();
        UpdateManager.getInstance().initialize();
    }

    static void postInitializeMods() {
        if (ModContainer.getActiveModContainer() != null)
            throw new IllegalStateException("postInitializeMods() called with active mod container");
        gameThread = Thread.currentThread();
        for (Mod mod : mods) {
            ModContainer.setActiveModContainer(mod.getModContainer());
            mod.onInit();
        }
        ModContainer.setActiveModContainer(null);
        for (Mod mod : mods) {
            ModContainer.setActiveModContainer(mod.getModContainer());
            mod.onPostInit();
        }
        ModContainer.setActiveModContainer(null);
        if (FoxLauncher.isClient() && ModLoaderOptions.INSTANCE.checkForUpdates) {
            UpdateManager.getInstance().checkUpdates();
        }
    }

    @Override
    public void onReceiveDataFromClient(@NotNull NetworkManager connection, byte[] data) throws IOException {
        LoaderNetworkManager.executeClientPacketData(connection, data);
    }

    @Override
    public void onReceiveDataFromServer(@NotNull NetworkManager connection, byte[] data) throws IOException {
        LoaderNetworkManager.executeServerPacketData(connection, data);
    }

    @Override
    public void onPreInit() {
        this.setConfigObject(ModLoaderOptions.INSTANCE);
        FoxPowerUtils.updateMaxSinkPriorityValue();
        CommandRegistry.registerClientCommand(new Command("fldebug2", false, false) {
            @Override
            public void onExecute(String[] args, ICommandListener commandExecutor) {
                commandExecutor.log(GameRegistry.Internal.state());
            }

            @Override
            public void printHelpInformation(ICommandListener iCommandListener) {

            }

            @Override
            public String commandSyntax() {
                return "";
            }
        });
    }

    @EventHandler
    public void onInitGui(GuiScreenInitEvent initGuiEvent) {
        GuiScreen guiScreen = initGuiEvent.getGuiScreen();
        if (guiScreen instanceof GuiMainMenu) {
            SidedMetadataAPI.Internal.setActiveMetaData(null);
        }
        if (guiScreen instanceof GuiConnecting) {
            SidedMetadataAPI.Internal.setActiveMetaData(Collections.emptyMap());
        }
        if (guiScreen instanceof GuiMainMenu || guiScreen instanceof GuiIngameMenu) {
            initGuiEvent.getControlList().add(new GuiButtonCallback(
                    500, guiScreen.width - 62, 2, 60, 20, "Mods", () ->
                    Minecraft.getInstance().displayGuiScreen(new GuiModMenu(guiScreen))));
        }
    }

    @EventHandler
    public void onLifecycle(LifecycleStartEvent lifecycleStartEvent) {
        FoxPowerUtils.updateMaxSinkPriorityValue();
    }

    public static boolean areAllModsLoaded() {
        return areAllModsLoaded;
    }

    public static Thread getGameThread() {
        return gameThread;
    }

    /**
     * Send message to either all op or to the player hosting the world, mainly used for critical error reporting.
     *
     * @param message the message to broadcast
     */
    public static void broadcastMessageToPrivileged(String message) {
        switch (FoxLauncher.getEnvironmentType()) {
            case CLIENT: {
                // Send message to self player in single player
                EntityPlayerSP entityPlayerSP = ((Minecraft) CoreConstants.CORE).thePlayer;
                if (entityPlayerSP != null) entityPlayerSP.addChatMessage(message);
                break;
            }
            case SERVER: {
                ((MinecraftServer) CoreConstants.CORE).configManager.sendChatMessageToAllOps(message);
                break;
            }
        }
    }

    public static final class Internal {
        private static boolean postInitializeModsTrigger = false;

        public static void postInitializeMods() {
            if (postInitializeModsTrigger)
                throw new IllegalStateException();
            postInitializeModsTrigger = true;
            ModLoader.postInitializeMods();
            if (ModLoaderInit.isClientDevModeImpl && // Fix error: [1D-10T]
                    !(FoxLauncher.DEV_MODE || FoxLauncher.DEVELOPING_FOXLOADER)) {
                Minecraft.theMinecraft = null;
            }
        }

        public static void debugMarker() {
            ModLoaderInit.getModLoaderLogger().info("Debug Marker Hit");
        }
    }
}
