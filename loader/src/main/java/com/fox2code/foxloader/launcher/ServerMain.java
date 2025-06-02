package com.fox2code.foxloader.launcher;

public final class ServerMain {
    public static void main(String[] args) throws Throwable {
        FoxLauncher.initForServer();
        FoxLauncher.runServerWithArgs(args);
    }
}
