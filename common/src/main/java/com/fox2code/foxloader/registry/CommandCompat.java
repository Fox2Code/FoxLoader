package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.network.NetworkPlayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandCompat {
    private static final HashMap<String, CommandCompat> commandsMap = new HashMap<>();
    private static final HashMap<String, CommandCompat> clientCommandsMap = new HashMap<>();
    public static final Map<String, CommandCompat> commands = Collections.unmodifiableMap(commandsMap);
    public static final Map<String, CommandCompat> clientCommands = Collections.unmodifiableMap(clientCommandsMap);
    public static final String[] NO_ALIASES = new String[0];

    public static void registerCommand(CommandCompat commandCompat) {
        commandsMap.put(commandCompat.name, commandCompat);
    }

    public static void registerClientCommand(CommandCompat commandCompat) {
        clientCommandsMap.put(commandCompat.name, commandCompat);
    }

    private final String name;
    private final boolean opOnly;
    private final boolean isHidden;
    private final String[] aliases;
    private final boolean supportConsole;

    public CommandCompat(String name) {
        this(name, true, false, NO_ALIASES);
    }

    public CommandCompat(String name, boolean opOnly) {
        this(name, opOnly, false, NO_ALIASES);
    }

    public CommandCompat(String name, boolean opOnly, boolean isHidden, String[] aliases) {
        this(name, opOnly, isHidden, aliases, null);
    }

    public CommandCompat(String name, boolean opOnly, boolean isHidden, String[] aliases, boolean supportConsole) {
        this(name, opOnly, isHidden, aliases, (Boolean) supportConsole);
    }

    private CommandCompat(String name, boolean opOnly, boolean isHidden, String[] aliases, Boolean supportConsole) {
        this.name = name;
        this.opOnly = opOnly;
        this.isHidden = isHidden;
        this.aliases = aliases == null ?
                NO_ALIASES : aliases;
        if (supportConsole == null) {
            try { // Auto-detect support if not explicitly defined.
                supportConsole = this.getClass().getMethod("onExecute",
                                String[].class, RegisteredCommandSender.class)
                        .getDeclaringClass() != CommandCompat.class;
            } catch (Throwable ignored) {}
        }
        this.supportConsole = supportConsole == Boolean.TRUE;
    }

    public final String getName() {
        return this.name;
    }

    public final boolean isOpOnly() {
        return this.opOnly;
    }

    public final boolean isHidden() {
        return this.isHidden;
    }

    public final String[] getAliases() {
        return this.aliases;
    }

    public final boolean supportConsole() {
        return this.supportConsole;
    }

    public void onExecute(String[] args, RegisteredCommandSender commandSender) {
        if (!this.supportConsole) {
            if (commandSender instanceof NetworkPlayer) {
                this.onExecute(args, (NetworkPlayer) commandSender);
            } else {
                commandSender.displayChatMessage("This command can only be executed by a player!");
            }
        }
    }

    public void onExecute(String[] args, NetworkPlayer commandExecutor) {
        if (this.supportConsole) {
            this.onExecute(args, (RegisteredCommandSender) commandExecutor);
        }
    }

    public void printHelpInformation(NetworkPlayer commandExecutor) {}

    public String commandSyntax() {
        return this.name;
    }
}
