package com.fox2code.foxloader.client;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.mitask.command.Command;
import net.minecraft.src.client.gui.StringTranslate;
import net.minecraft.src.client.player.EntityPlayerSP;

public final class ClientCommandWrapper extends Command {
    private final CommandCompat commandCompat;

    public ClientCommandWrapper(CommandCompat commandCompat) {
        super(commandCompat.getName(), commandCompat.isOpOnly(), commandCompat.isHidden(), commandCompat.getAliases());
        this.commandCompat = commandCompat;
    }

    @Override
    public void onExecute(String[] args, EntityPlayerSP commandExecutor) {
        try {
            this.commandCompat.onExecute(args, (NetworkPlayer) commandExecutor);
        } catch (Throwable t) {
            t.printStackTrace();
            ((NetworkPlayer) commandExecutor).displayChatMessage(
                    StringTranslate.getInstance().translateKey("command.error.internal-error"));
        }
    }

    @Override
    public void printHelpInformation(EntityPlayerSP entityPlayerSP) {
        this.commandCompat.printHelpInformation((NetworkPlayer) entityPlayerSP);
    }

    @Override
    public String commandSyntax() {
        return this.commandCompat.commandSyntax();
    }
}
