package net.minecraft.server.plugin;

import net.minecraft.server.command.Command;
import net.minecraft.server.command.CommandRegistry;

// https://git.derekunavailable.direct/Dereku/ReIndevPatches
public class JavaPlugin {
    @Deprecated
    public String getName() {
        throw new UnsupportedOperationException();
    }

    public boolean registerCommand(Command command, boolean override) {
        return CommandRegistry.getInstance().registerCommand(this, command, override);
    }
}
