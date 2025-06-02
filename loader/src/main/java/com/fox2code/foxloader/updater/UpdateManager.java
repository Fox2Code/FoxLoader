package com.fox2code.foxloader.updater;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import net.minecraft.common.util.ChatColors;

import javax.swing.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
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
    private WeakReference<JButton> foxLoaderUpdateButton;
    private boolean initialized = false;
    private boolean hasUpdates = false;
    private boolean canUpdate = false;
    private boolean checkingUpdates = false;
    private boolean hasCheckedUpdates = false;

    private UpdateManager() {}

    public synchronized void initialize() {
        if (this.initialized) return;
        if (!ModLoader.areAllModsLoaded()) {
            throw new IllegalStateException("Mods are not yet loaded!");
        }
        for (ModContainer modContainer : ModLoaderInit.getModContainers()) {
            AbstractUpdater abstractUpdater;
            for (Function<ModContainer, AbstractUpdater> provider : this.providers) {
                if ((abstractUpdater = provider.apply(modContainer)) != null &&
                        abstractUpdater.modContainer == modContainer) {
                    modsToUpdater.put(modContainer.getModId(), abstractUpdater);
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

    public void bindButtonToFoxLoaderUpdate(JButton button) {
        button.setEnabled(this.hasUpdate("foxloader"));
        this.foxLoaderUpdateButton = new WeakReference<>(button);
        // This is used in error screen, which means we may need to initialize stuff here
        if (!this.initialized) {
            this.initialize();
        }
        if (!this.hasCheckedUpdates) {
            this.checkUpdates();
        }
    }

    public void checkUpdates() {
        if (!this.initialized) throw new IllegalStateException("UpdateManager was not initialized yet!");
        if (this.checkingUpdates) return;
        this.hasCheckedUpdates = true;
        this.checkingUpdates = true;
        new Thread(this::checkUpdates0, "FoxLoader - Update Checker Thread").start();
    }

    private synchronized void checkUpdates0() {
        this.checkingUpdates = true;
        this.hasUpdates = false;
        this.canUpdate = false;
        boolean online = false;
        try {
            online = InetAddress.getByName("www.jitpack.io") != null;
        } catch (UnknownHostException ignored) {}
        if (!online) {
            ModLoaderInit.getModLoaderLogger()
                    .log(Level.INFO, "Skipping update checking because we are offline...");
            this.checkingUpdates = false;
            return;
        }
        boolean hasUpdates = false, canUpdate = false;
        for (AbstractUpdater abstractUpdater : modsToUpdater.values()) {
            if (abstractUpdater.updateConsumed) continue;
            try {
                abstractUpdater.latestVersion = abstractUpdater.findLatestVersion();
                if (abstractUpdater.hasUpdate()) {
                    hasUpdates = true;
                    canUpdate |= abstractUpdater.canUpdate();
                }
            } catch (Exception e) {
                abstractUpdater.latestVersion = null;
                ModLoaderInit.getModLoaderLogger()
                        .log(Level.WARNING, "Update check failed for " +
                                abstractUpdater.modContainer.getModId() + "!", e);
            }
        }
        this.hasUpdates = hasUpdates;
        this.canUpdate = canUpdate;
        this.checkingUpdates = false;
        AbstractUpdater abstractUpdater = modsToUpdater.get("foxloader");
        // If FoxLoader is wrongly installed, try to fix it
        if (FoxLauncher.isWronglyInstalled() &&
                FoxLauncher.getLauncherType().hasAutoFix) {
            System.out.println("It look like you were too incompetent to install FoxLoader properly");
            System.out.println("But don't worry, FoxLoader will install itself properly on the current instance");
            if (abstractUpdater != null) {
                try {
                    abstractUpdater.doUpdate();
                } catch (IOException e) {
                    ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                            "Failed to update " + abstractUpdater.modContainer.getModName(), e);
                }
            }
        } else {
            final JButton button = this.foxLoaderUpdateButton == null ?
                    null : this.foxLoaderUpdateButton.get();
            if (button != null) {
                boolean hasUpdate = this.hasUpdate("foxloader");
                button.setEnabled(hasUpdate);
                if (hasUpdate) {
                    button.addActionListener(event -> {
                        button.setEnabled(false);
                        try {
                            abstractUpdater.doUpdate();
                        } catch (IOException e) {
                            ModLoaderInit.getModLoaderLogger().log(
                                    Level.WARNING, "Failed to update FoxLoader", e);
                        }
                    });
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
                    ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Update failed!", e);
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
                ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Update failed!", e);
            }
        }
    }

    public boolean hasUpdates() {
        return this.hasUpdates;
    }

    public boolean canUpdate() {
        return this.canUpdate;
    }

    public boolean hasUpdate(String modId) {
        if (!this.hasUpdates) return false;
        AbstractUpdater abstractUpdater =
                this.modsToUpdater.get(modId);
        return abstractUpdater != null &&
                abstractUpdater.hasUpdate();
    }
    public boolean canUpdate(String modId) {
        if (!this.canUpdate) return false;
        AbstractUpdater abstractUpdater =
                this.modsToUpdater.get(modId);
        return abstractUpdater != null &&
                abstractUpdater.hasUpdate() &&
                abstractUpdater.canUpdate();
    }

    public UpdateState getUpdateState(String modId) {
        AbstractUpdater abstractUpdater =
                this.modsToUpdater.get(modId);
        if (abstractUpdater == null)
            return UpdateState.UP_TO_DATE;
        if (abstractUpdater.updateConsumed)
            return UpdateState.UPDATED;
        if (this.hasUpdates && abstractUpdater.hasUpdate()) {
            return abstractUpdater.canUpdate() ?
                    UpdateState.UPDATABLE : UpdateState.HAS_UPDATE;
        }
        return UpdateState.UP_TO_DATE;
    }

    public enum UpdateState {
        UP_TO_DATE(ChatColors.RESET),
        HAS_UPDATE(ChatColors.RAINBOW + ChatColors.ITALIC),
        UPDATABLE(ChatColors.RAINBOW),
        UPDATED(ChatColors.RAINBOW + ChatColors.BOLD);

        public final String colorPrefix;

        UpdateState(String colorPrefix) {
            this.colorPrefix = colorPrefix;
        }
    }
}
