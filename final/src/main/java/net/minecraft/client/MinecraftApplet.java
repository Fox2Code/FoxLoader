package net.minecraft.client;

import com.fox2code.foxloader.installer.InstallerGUI;
import com.fox2code.foxloader.installer.InstallerPlatform;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.DependencyHelper;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.LauncherType;

import javax.swing.*;
import java.applet.Applet;
import java.io.IOException;

public class MinecraftApplet extends Applet {
    static {
        if (FoxLauncher.getFoxClassLoader() == null &&
                FoxLauncher.getLauncherType() != LauncherType.BETA_CRAFT &&
                System.getProperty("foxloader.internal.betacraft-wrapper") == null) {
            FoxLauncher.markWronglyInstalledUnrecoverable();
            LauncherType launcherType = FoxLauncher.getLauncherType();
            System.out.println("Auto-detected launcher type: " + launcherType);

            if (launcherType == LauncherType.UNKNOWN) {
                String dirName = FoxLauncher.foxLoaderFile.getParentFile().getName();
                System.out.println("Directory name: \"" + dirName + "\"");
                System.out.println("Path: \"" + FoxLauncher.foxLoaderFile.getPath() + "\"");
                if (dirName.equals("libraries") || dirName.equals("jarmods")) {
                    launcherType = LauncherType.MMC_LIKE;
                } else if (dirName.equals("bin")) {
                    launcherType = LauncherType.BIN;
                } else {
                    DependencyHelper.setMCLibraryRoot();
                    launcherType = FoxLauncher.getLauncherType();
                    if (launcherType == LauncherType.UNKNOWN)
                        launcherType = LauncherType.BETA_CRAFT;
                }
            }
            System.out.println("Detected launcher type: " + launcherType);
            if (launcherType.hasAutoFix) {
                try {
                    new InstallerGUI(InstallerPlatform.DEFAULT, launcherType).doSilentInstall(null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                JOptionPane.showConfirmDialog(null, "You failed to install FoxLoader properly.\n" +
                                "But don't worry, FoxLoader automatically installed itself for you!\n" +
                                "You should have a FoxLoader instance the next time you open the " +
                                launcherType + " Launcher",
                        "FoxLoader " + BuildConfig.REINDEV_VERSION + " (Improper install)",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            } else {
                JOptionPane.showConfirmDialog(null, "You failed to install FoxLoader properly.\n" +
                                "FoxLoader wasn't able to recover from this issue\n" +
                                "You need to run the jar file or drag and drop the zip to install FoxLoader",
                        "FoxLoader " + BuildConfig.REINDEV_VERSION + " (Improper install)",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        }
    }
}
