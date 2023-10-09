package net.minecraft.server.command;

public class CommandRegistry {
    private static final CommandRegistry INSTANCE = new CommandRegistry();

    private CommandRegistry() {}

    public static CommandRegistry getInstance() {
        return INSTANCE;
    }

    public void registerCommand(String label, Command command) {
    }
}
