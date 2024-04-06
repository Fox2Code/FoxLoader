package com.fox2code.foxloader.commands;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.RegisteredItemStack;

public class SetWorldItemScale extends CommandCompat {
    public SetWorldItemScale() {
        super("set_world_item_scale", true, false, new String[]{"set_item_scale"});
    }

    @Override
    public void onExecute(String[] args, NetworkPlayer commandExecutor) {
        if (args.length < 2) {
            commandExecutor.displayChatMessage("command.set-item-scale.missing-argument");
            return;
        }
        float newScale = "reset".equals(args[1]) ? -1F : Float.parseFloat(args[1]);
        if (commandExecutor.getRegisteredHeldItem() == null) {
            commandExecutor.displayChatMessage("command.error.missing-item");
        } else if (newScale == -1F || newScale == 0F) {
            commandExecutor.getRegisteredHeldItem().resetWorldItemScale();
            commandExecutor.displayChatMessage("command.done");
        } else if (RegisteredItemStack.MINIMUM_WORLD_ITEM_SCALE <= newScale) {
            commandExecutor.getRegisteredHeldItem().setWorldItemScale(newScale);
            commandExecutor.displayChatMessage("command.done");
        } else {
            commandExecutor.displayChatMessage("command.set-item-scale.out-of-range");
        }
    }
}
