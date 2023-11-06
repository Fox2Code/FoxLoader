package com.fox2code.foxloader.updater;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.FLJitPackUpdater;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.ChatColors;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.logging.Level;

public final class UpdateManager {
    private static final UpdateManager INSTANCE = new UpdateManager();

    public static UpdateManager getInstance() {
        return INSTANCE;
    }

    private final HashMap<String, AbstractUpdater> modsToUpdater = new HashMap<>();
    private final LinkedList<Function<ModContainer, AbstractUpdater>> providers = new LinkedList<>();
    private boolean initialized = false;
    private boolean hasUpdates = false;
    private boolean checkingUpdates = false;

    private UpdateManager() {}

    public synchronized void initialize() {
        if (this.initialized) return;
        this.providers.add(modContainer ->
                modContainer.jitpack == null ? null :
                        new JitPackUpdater(modContainer));
        for (ModContainer modContainer : ModLoader.getModContainers()) {
            if (modContainer.id.equals(ModLoader.FOX_LOADER_MOD_ID)) {
                modsToUpdater.put(modContainer.id, new FLJitPackUpdater());
                continue;
            }
            AbstractUpdater abstractUpdater;
            for (Function<ModContainer, AbstractUpdater> provider : this.providers) {
                if ((abstractUpdater = provider.apply(modContainer)) != null &&
                        abstractUpdater.modContainer == modContainer) {
                    modsToUpdater.put(modContainer.id, abstractUpdater);
                    break;
                }
            }
        }

        this.initialized = true;
    }

    public void registerAbstractUpdater(AbstractUpdater updater) {
        if (updater == null) return;
        this.registerProvider(modContainer -> updater.modContainer == modContainer ? updater : null);
    }

    public void registerProvider(Function<ModContainer, AbstractUpdater> provider) {
        if (provider == null) return;
        if (this.initialized) throw new IllegalArgumentException("UpdateManager is already initialized!");
        this.providers.add(provider);
    }

    public void checkUpdates() {
        if (this.checkingUpdates) return;
        new Thread(this::checkUpdates0, "FoxLoader - Update Checker Thread").start();
    }

    private synchronized void checkUpdates0() {
        if (!this.initialized) throw new IllegalArgumentException("UpdateManager was not initialized yet!");
        this.checkingUpdates = true;
        this.hasUpdates = false;
        boolean online = false;
        try {
            online = InetAddress.getByName("www.jitpack.io") != null;
        } catch (UnknownHostException ignored) {}
        if (!online) {
            ModLoader.getModContainer(ModLoader.FOX_LOADER_MOD_ID).logger
                    .log(Level.INFO, "Skipping update checking because we are offline...");
            this.checkingUpdates = false;
            return;
        }
        boolean hasUpdates = false;
        for (AbstractUpdater abstractUpdater : modsToUpdater.values()) {
            if (abstractUpdater.updateConsumed) continue;
            try {
                abstractUpdater.latestVersion = abstractUpdater.findLatestVersion();
                hasUpdates |= abstractUpdater.hasUpdate();
            } catch (Exception e) {
                abstractUpdater.latestVersion = null;
                ModLoader.getModContainer(ModLoader.FOX_LOADER_MOD_ID)
                        .logger.log(Level.WARNING, "Update check failed for " +
                                abstractUpdater.modContainer.id + "!", e);
            }
        }
        this.hasUpdates = hasUpdates;
        this.checkingUpdates = false;
        // If FoxLoader is wrongly installed, try to fix it
        if (FoxLauncher.isWronglyInstalled() &&
                FoxLauncher.getLauncherType().hasAutoFix) {
            System.out.println("It look like you were too incompetent to install FoxLoader properly");
            System.out.println("But don't worry, FoxLoader will install itself properly on the current instance");
            AbstractUpdater abstractUpdater = modsToUpdater.get("foxloader");
            if (abstractUpdater != null) {
                try {
                    abstractUpdater.doUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void doUpdates() {
        if (!this.hasUpdates) return;
        for (AbstractUpdater abstractUpdater : modsToUpdater.values()) {
            if (abstractUpdater.updateConsumed) continue;
            abstractUpdater.updateConsumed = true;
            if (abstractUpdater.hasUpdate()) {
                try {
                    abstractUpdater.doUpdate();
                } catch (IOException e) {
                    ModLoader.getModContainer(ModLoader.FOX_LOADER_MOD_ID)
                            .logger.log(Level.WARNING, "Update failed!", e);
                }
            }
        }
    }
    public synchronized void doUpdate(String modId) {
        if (!this.hasUpdates) return;
        AbstractUpdater abstractUpdater =
                this.modsToUpdater.get(modId);
        if (abstractUpdater != null && abstractUpdater.hasUpdate()
                && !abstractUpdater.updateConsumed) {
            abstractUpdater.updateConsumed = true;
            try {
                abstractUpdater.doUpdate();
            } catch (IOException e) {
                ModLoader.getModContainer(ModLoader.FOX_LOADER_MOD_ID)
                        .logger.log(Level.WARNING, "Update failed!", e);
            }
        }
    }

    public boolean hasUpdates() {
        return this.hasUpdates;
    }

    public boolean hasUpdate(String modId) {
        if (!this.hasUpdates) return false;
        AbstractUpdater abstractUpdater =
                this.modsToUpdater.get(modId);
        return abstractUpdater != null &&
                abstractUpdater.hasUpdate();
    }

    public UpdateState getUpdateState(String modId) {
        AbstractUpdater abstractUpdater =
                this.modsToUpdater.get(modId);
        if (abstractUpdater == null)
            return UpdateState.UP_TO_DATE;
        if (abstractUpdater.updateConsumed)
            return UpdateState.UPDATED;
        if (this.hasUpdates && abstractUpdater.hasUpdate()) {
            return UpdateState.UPDATABLE;
        }
        return UpdateState.UP_TO_DATE;
    }

    public enum UpdateState {
        UP_TO_DATE(ChatColors.RESET),
        UPDATABLE(ChatColors.RAINBOW),
        UPDATED(ChatColors.RAINBOW + ChatColors.BOLD);

        public final String colorPrefix;

        UpdateState(String colorPrefix) {
            this.colorPrefix = colorPrefix;
        }
    }
}