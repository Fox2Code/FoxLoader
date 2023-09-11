package com.fox2code.foxloader.launcher;

/**
 * If you want me to support your custom launcher, please open a new issue!
 */
public enum LauncherType {
    /**
     * Ex: Server ran via --server
     */
    UNKNOWN(false),
    /**
     * BIN is a special broken case that can happen on MultiMC
     * when users lacks the mental capabilities to run a jar file
     */
    BIN(false),
    /**
     * Ex: Dev environment.
     */
    GRADLE(false),
    /**
     * Ex: Vanilla launcher & Pojav launcher
     */
    VANILLA_LIKE(false),
    /**
     * Ex: BetaCraft launcher.
     */
    BETA_CRAFT(true),
    /**
     * Ex: MultiMC/PolyMC/PrismLauncher
     */
    MMC_LIKE(true);

    public final boolean hasAutoFix;

    LauncherType(boolean hasAutoFix) {
        this.hasAutoFix = hasAutoFix;
    }
}
