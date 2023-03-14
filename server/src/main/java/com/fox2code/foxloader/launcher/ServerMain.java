package com.fox2code.foxloader.launcher;

public class ServerMain {
    public static void main(String[] args) throws ReflectiveOperationException {
        FoxLauncher.initForServerFromArgs(args);
        FoxLauncher.runServerWithArgs(args);
    }
}
