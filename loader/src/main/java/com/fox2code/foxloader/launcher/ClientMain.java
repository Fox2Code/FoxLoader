package com.fox2code.foxloader.launcher;

public final class ClientMain {
    public static void main(String... args) throws Throwable {
        if (FoxLauncher.launcherType == LauncherType.UNKNOWN &&
                FoxLauncher.foxLoaderFile.getParentFile().getName().equals("libraries")) {
            FoxLauncher.launcherType = LauncherType.MMC_LIKE;
        }
        FoxLauncher.initForClientFromArgs(args);
        FoxLauncher.runClientWithArgs(args);
    }
}
