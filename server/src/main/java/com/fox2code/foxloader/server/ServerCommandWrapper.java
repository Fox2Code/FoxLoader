package com.fox2code.foxloader.server;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.mitask.command.Command;
import net.minecraft.src.game.entity.player.EntityPlayerMP;

public final class ServerCommandWrapper extends Command {
    private final CommandCompat commandCompat;

    public ServerCommandWrapper(CommandCompat commandCompat) {
        super(commandCompat.getName(), commandCompat.isOpOnly(), commandCompat.isHidden(), commandCompat.getAliases());
        this.commandCompat = commandCompat;
    }

    @Override
    public void onExecute(String[] args, EntityPlayerMP commandExecutor) {
        this.commandCompat.onExecute(args, (NetworkPlayer) commandExecutor);
    }

    @Override
    public void printHelpInformation(EntityPlayerMP entityPlayerSP) {
        this.commandCompat.printHelpInformation((NetworkPlayer) entityPlayerSP);
    }

    @Override
    public String commandSyntax() {
        return this.commandCompat.commandSyntax();
    }
}
