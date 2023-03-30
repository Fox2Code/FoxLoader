package com.fox2code.foxloader.launcher;

/**
 * If you want me to support your custom launcher, please open a new issue!
 */
public enum LauncherType {
    /**
     * Ex: Server ran via --server
     */
    UNKNOWN,
    /**
     * Ex: Dev environment.
     */
    GRADLE,
    /**
     * Ex: Vanilla launcher & Pojav launcher
     */
    VANILLA_LIKE,
    /**
     * Ex: BetaCraft launcher.
     */
    BETA_CRAFT,
    /**
     * Ex: MultiMC/PolyMC/PrismLauncher
     */
    MMC_LIKE
}
