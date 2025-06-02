package com.fox2code.foxloader.spark;

import net.minecraft.common.command.Command;
import net.minecraft.common.command.ICommandListener;
import net.minecraft.common.command.IllegalCmdListenerOperation;

abstract class FoxLoaderSparkCommand extends Command {
    public FoxLoaderSparkCommand(String name, boolean opOnly) {
        super(name, opOnly, false);
    }

    public FoxLoaderSparkCommand(String name, boolean opOnly, String... aliases) {
        super(name, opOnly, false, aliases);
    }


    @Override
    public abstract void onExecute(String[] args, ICommandListener commandExecutor) throws IllegalCmdListenerOperation;

    @Override
    public void printHelpInformation(ICommandListener iCommandListener) {

    }

    @Override
    public String commandSyntax() {
        return "§e" + this.getName();
    }
}
