package com.fox2code.foxloader.commands;

import com.fox2code.foxloader.loader.packet.ServerDynamicTexture;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.RegisteredItemStack;

public class SetDynTex extends CommandCompat {
    public SetDynTex() {
        super("setdyntex");
    }

    @Override
    public void onExecute(String[] args, NetworkPlayer commandExecutor) {
        if (args.length < 2) {
            commandExecutor.displayChatMessage("Missing Dynamic texture id argument");
            return;
        }
        int dynTexId = Integer.parseInt(args[1]);
        if (dynTexId < -1 || dynTexId >= ServerDynamicTexture.SERVER_DYN_MAX_ID) {
            commandExecutor.displayChatMessage("Dynamic texture id must be between 0 and " +
                    ServerDynamicTexture.SERVER_DYN_MAX_ID + " (-1 remove dynamic texturing)");
            return;
        }
        RegisteredItemStack itemStack = commandExecutor.getRegisteredHeldItem();
        if (itemStack == null) {
            commandExecutor.displayChatMessage("You need to be holding an item to run this command");
        } else {
            itemStack.setRegisteredDynamicTextureId(dynTexId);
            commandExecutor.displayChatMessage("Changed dynamic texture of item to " + dynTexId);
        }
    }
}
