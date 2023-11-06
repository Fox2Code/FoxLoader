package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.launcher.ServerMain;
import com.fox2code.foxloader.launcher.utils.SourceUtil;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Locale;

public class Main {
    // We need -XX:+IgnoreUnrecognizedVMOptions cause some of the optimization arg we us are not available on some JVMs
    static final String reqJvmArgs = "-XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockExperimentalVMOptions " +
            "-XX:+DisableAttachMechanism -Dcom.ibm.tools.attach.enable=no -Dfile.encoding=UTF-8";
    static final String optJvmArgs = reqJvmArgs + " -XX:+UseFastAccessorMethods -XX:+AggressiveOpts " +
            "-XX:+UseCompressedOops -XX:+UseBiasedLocking -XX:+OptimizeStringConcat -XX:-UseGCOverheadLimit " +
            "-XX:+UseStringCache -XX:+UseCompressedStrings";
    static final String optJvmArgsWithMem = optJvmArgs + " -Xmn512M -Xms512M -Xmx2G";
    static final File currentInstallerFile = SourceUtil.getSourceFile(Main.class);
    public static void main(String[] args) throws ReflectiveOperationException, MalformedURLException {
        if (args.length == 0 && GraphicsEnvironment.isHeadless()) {
            ServerMain.main(args);
            return;
        }
        boolean platform = false;
        boolean update = false;
        if (args.length >= 1) {
            boolean test = false;
            boolean with = false;
            boolean server = false;
            switch (args[0]) {
                default:
                    System.out.println("Unknown argument: " + args[0]);
                    return;
                case "--help":
                    System.out.println("--help -> Show this page");
                    System.out.println("--server -> Start server");
                    System.out.println("--with-server -> Start server with specified server jar");
                    System.out.println("--test-server -> Like --with-server but only load the strict necessary for ");
                    return;
                case "--platform":
                    platform = true;
                    break;
                case "--update":
                    update = true;
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
        if (update || installerPlatform.doSilentInstall) {
            System.setErr(System.out); // Redirect errors to stdout
            LauncherType launcherType = !update ? LauncherType.VANILLA_LIKE :
                    LauncherType.valueOf(args[1].toUpperCase(Locale.ROOT));
            try {
                System.out.println("Updating...");
                new InstallerGUI(installerPlatform, launcherType)
                        .doSilentInstall(args.length >= 3 ? args[2] : null);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            return;
        }
        new InstallerGUI(installerPlatform).show();
    }

    public static boolean isPojavLauncherHome(String userHome) {
        int index;
        return (userHome.startsWith("/storage/emulated/") && (index = userHome.indexOf('/', 18)) != -1 &&
                userHome.substring(index).startsWith("/Android/data/") || userHome.startsWith("/sdcard/Android/data/"));
    }

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.language", "en");
    }
}
