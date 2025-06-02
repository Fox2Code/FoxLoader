package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.utils.Platform;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class ClientAndServerMain {
    static {
        System.setProperty("foxloader.logger-distinguish-side", "true");
    }

    public static void main(String[] args) throws Throwable {
        ArrayList<String> command = new ArrayList<>();
        if (Platform.getPlatform() == Platform.WINDOWS) {
            command.add(new File(System.getProperty("java.home") + "\\bin\\javaw.exe").getAbsolutePath());
        } else {
            command.add(new File(System.getProperty("java.home") + "/bin/java").getAbsolutePath());
        }
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-Dfoxloader.logger-distinguish-side=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("com.fox2code.foxloader.launcher.ServerMain");
        Process serverProcess = new ProcessBuilder(command).inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (serverProcess.isAlive()) {
                serverProcess.destroy();
            }
        }, "Server subprocess killer"));
        ClientMain.main(args);
        System.exit(serverProcess.waitFor());
    }
}
