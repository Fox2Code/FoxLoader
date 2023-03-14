package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.launcher.utils.SourceUtil;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Map;

public class ClientMain {
    private static final File currentLoaderFile = SourceUtil.getSourceFile(ClientMain.class);
    public static boolean hasBetaCraftDiscordRPC = false;

    /**
     * This is executed by FoxLoader BetaCraft wrapper.
     */
    public static void mainBetaCraft(Map<String, String> params, String gameFolder,
                                     boolean hasDiscordRPC) throws ReflectiveOperationException {
        // When on BetaCraft, restore original out/err stream to install our custom one.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        hasBetaCraftDiscordRPC = hasDiscordRPC;
        String name = currentLoaderFile.getName();
        int i = name.indexOf("-with-");
        if (i != -1) {
            try {
                FoxLauncher.setEarlyMinecraftURL(new File(
                        currentLoaderFile.getParentFile(),
                        name.substring(i + 6)).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        main(params.get("username"), params.get("sessionid"), "--gameFolder", gameFolder);
    }

    public static void main(String... args) throws ReflectiveOperationException {
        FoxLauncher.initForClientFromArgs(args);
        FoxLauncher.runClientWithArgs(args);
    }
}
