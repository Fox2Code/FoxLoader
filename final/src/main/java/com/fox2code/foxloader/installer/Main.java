package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.launcher.ServerMain;
import com.fox2code.foxloader.launcher.utils.SourceUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Locale;

public class Main {
    static final File currentWorkingDir = new File("").getAbsoluteFile();
    static final File currentInstallerFile = SourceUtil.getSourceFile(Main.class);
    public static void main(String[] args) throws ReflectiveOperationException, MalformedURLException {
        boolean platform = false;
        if (args.length >= 1) {
            boolean test = false;
            boolean with = false;
            boolean server = false;
            switch (args[0]) {
                default:
                    System.out.println("Unknown argument: " + args[0]);
                    return;
                case "--help":
                    System.out.println("--help -> This this page");
                    System.out.println("--server -> Start server");
                    System.out.println("--with-server -> Start server with specified server jar");
                    System.out.println("--test-server -> Like --with-server but only load the strict necessary for ");
                    return;
                case "--platform":
                    platform = true;
                    break;
                case "--test-server":
                    test = true;
                case "--with-server":
                    with = true;
                case "--server":
                    server = true;
            }

            if (test) {
                System.setProperty("foxloader.test-mode", "true");
            }
            if (with) {
                File file = new File(args[1]).getAbsoluteFile();
                if (!file.exists()) {
                    System.out.println("Input file doesn't exists");
                    return;
                }
                FoxLauncher.setEarlyMinecraftURL(file.toURI().toURL());
            }
            if (server) {
                int move = test ? 2 : 1;
                System.arraycopy(args, move, args, 0, args.length - move);
                ServerMain.main(Arrays.copyOf(args, args.length - move));
                return;
            }
        }

        InstallerPlatform installerPlatform = InstallerPlatform.DEFAULT;
        if (isPojavLauncherHome(System.getProperty("user.home"))) {
            installerPlatform = InstallerPlatform.POJAV_LAUNCHER;
        }
        if (platform) {
            installerPlatform = InstallerPlatform.valueOf(args[1].toUpperCase(Locale.ROOT));
        }
        new InstallerGUI(installerPlatform).show();
    }

    public static boolean isPojavLauncherHome(String userHome) {
        int index;
        return (userHome.startsWith("/storage/emulated/") && (index = userHome.indexOf('/', 18)) != -1 &&
                userHome.substring(index).startsWith("/Android/data/") || userHome.startsWith("/sdcard/Android/data/"));
    }
}
