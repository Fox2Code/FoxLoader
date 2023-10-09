package com.fox2code.foxloader.server;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.CommandRegistry;
import net.minecraft.server.command.CommandSender;
import net.minecraft.server.command.IssuerRole;

/**
 * Compatibility class used in ReIndevPatches
 */
public class ServerCommandWrapper4ReIndevPatches extends Command {

    private final CommandCompat commandCompat;

    private ServerCommandWrapper4ReIndevPatches(CommandCompat commandCompat) {
        super(commandCompat.getName(), commandCompat.getAliases());
        this.setIssuerRole(IssuerRole.PLAYER); // We don't support console :( // Sad to read :(
        this.commandCompat = commandCompat;
    }

    @Override
    public void execute(String commandLabel, String[] args, CommandSender commandSender) {
        NetworkPlayer networkPlayer = null;
        if (commandSender.isPlayer()) {
            try {
                networkPlayer = (NetworkPlayer) commandSender.getPlayer();
            } catch (Exception ignored) {}
        }
        if (networkPlayer == null) {
            commandSender.sendMessage("FoxLoader defined commands can only be executed by players!");
            return;
        }
        final String[] foxArgs = new String[args.length + 1];
        foxArgs[0] = commandLabel;
        System.arraycopy(args, 0, foxArgs, 1, args.length);
        this.commandCompat.onExecute(foxArgs, networkPlayer);
    }

    @Override
    public boolean onlyForOperators() {
        return this.commandCompat.isOpOnly();
    }

    @Override
    public boolean hideCommandArgs() {
        return this.commandCompat.isHidden();
    }

    public static void registerAllForReIndevPatches() {
        if (!ModLoader.areAllModsLoaded()) // Just a safeguard, just in case.
            throw new IllegalArgumentException("Not Loaded?");
        for (CommandCompat commandCompat : CommandCompat.commands.values()) {
            CommandRegistry.getInstance().registerCommand(commandCompat.getName(), new ServerCommandWrapper4ReIndevPatches(commandCompat));
        }
    }
}
