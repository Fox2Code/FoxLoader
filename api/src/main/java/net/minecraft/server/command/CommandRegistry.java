package net.minecraft.server.command;

import net.minecraft.server.plugin.JavaPlugin;

public class CommandRegistry {
    private static final CommandRegistry INSTANCE = new CommandRegistry();

    private CommandRegistry() {}

    public static CommandRegistry getInstance() {
        return INSTANCE;
    }

    public boolean registerCommand(JavaPlugin owner, Command command, boolean override) {
        return true;
    }
}
