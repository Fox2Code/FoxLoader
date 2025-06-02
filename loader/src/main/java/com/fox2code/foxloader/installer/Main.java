package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.launcher.ServerMain;
import com.fox2code.foxloader.utils.SourceUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public class Main {
    static final File currentInstallerFile = SourceUtil.getSourceFile(Main.class);
    public static void main(String[] args) throws Throwable {
        if (args.length == 0 && GraphicsEnvironment.isHeadless()) {
            ServerMain.main(new String[]{"nogui"});
            return;
        }
        boolean platform = false;
        boolean update = false;
        if (args.length >= 1) {
            boolean server = false;
            boolean nogui = false;
            switch (args[0]) {
                case "--help":
                    System.out.println("--help -> Show this page");
                    System.out.println("--server -> Start server");
                    return;
                case "nogui":
                case "--nogui":
                    server = true;
                    nogui = true;
                    break;
                case "--platform":
                    platform = true;
                    break;
                case "--update":
                    update = true;
                    break;
                case "--server":
                    server = true;
                    break;
                default:
                    System.out.println("Unknown argument: " + args[0]);
                    return;
            }

            if (server) {
                if (nogui) {
                    ServerMain.main(new String[]{"nogui"});
                } else {
                    System.arraycopy(args, 1, args, 0, args.length - 1);
                    ServerMain.main(Arrays.copyOf(args, args.length - 1));
                }
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
        if (update || installerPlatform.doSilentInstall) {
            System.setErr(System.out); // Redirect errors to stdout
            LauncherType launcherType = !update ? LauncherType.VANILLA_LIKE :
                    LauncherType.valueOf(args[1].toUpperCase(Locale.ROOT));
            try {
                System.out.println("Updating...");
                new InstallerGUI(installerPlatform, launcherType).doSilentInstall();
            } catch (IOException e) {
                e.printStackTrace(System.out);
                System.exit(-1);
            }
            return;
        }
        new InstallerGUI(installerPlatform).show();
    }

    public static boolean isPojavLauncherHome(String userHome) {
        int index; String ext;
        return ((userHome.startsWith("/storage/emulated/") && (index = userHome.indexOf('/', 18)) != -1 &&
                ((ext = userHome.substring(index)).startsWith("/Android/data/") || ext.startsWith("/games/"))) ||
                userHome.startsWith("/sdcard/Android/data/") || userHome.startsWith("/sdcard/games/"));
    }

    static {
        // We can only run the server from there
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.language", "en");
    }
}
