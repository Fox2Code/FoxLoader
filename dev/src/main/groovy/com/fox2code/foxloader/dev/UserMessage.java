package com.fox2code.foxloader.dev;

public final class UserMessage extends Throwable {
    public static final UserMessage UNRECOVERABLE_STATE_DECOMPILE = new UserMessage("---",
            "Daemon was in an unrecoverable state for decompile and was terminated.",
            "if after reloading the project the problem persists you can add",
            "\"foxloader.decompileSources = false\" to your build.gradle to disable",
            "ReIndev source code decompilation entirely");
    public static final UserMessage FAIL_DECOMPILE_CLIENT = new UserMessage(true);
    public static final UserMessage FAIL_DECOMPILE_SERVER = new UserMessage(false);

    private UserMessage(boolean client) {
        this("---",
                "An error happened while trying to decompile ReIndev " + (client ? "client" : "server"),
                "FoxLoader development plugin tried to solve the invalid state for you, but",
                "if after reloading the project the problem persists you can add",
                "\"foxloader.decompileSources = false\" to your build.gradle to disable",
                "ReIndev source code decompilation entirely");
    }

    public UserMessage(String... strings) {
        super(format(strings), null);
    }

    private static String format(String[] strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : strings) {
            stringBuilder.append('\n').append(string);
        }
        return stringBuilder.toString();
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
