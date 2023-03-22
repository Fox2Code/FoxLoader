package com.fox2code.foxloader.installer;

public enum InstallerPlatform {
    DEFAULT(false, false, false),
    FULLSCREEN_TEST(true, true, false),
    POJAV_LAUNCHER(false, true, true, "Pojav");

    public final boolean fullscreen;
    public final boolean fullscreenLayout;
    public final boolean specialLauncher;
    public final String platformName;

    InstallerPlatform(boolean fullscreen, boolean fullscreenLayout,
                      boolean specialLauncher) {
        this(fullscreen, fullscreenLayout, specialLauncher, "Minecraft");
    }

    InstallerPlatform(boolean fullscreen, boolean fullscreenLayout,
                      boolean specialLauncher, String platformName) {
        if (fullscreen && !fullscreenLayout) {
            throw new IllegalArgumentException("Fullscreen layout required to allow fullscreen frame!");
        }
        this.fullscreen = fullscreen;
        this.fullscreenLayout = fullscreenLayout;
        this.specialLauncher = specialLauncher;
        this.platformName = platformName;
    }
}
