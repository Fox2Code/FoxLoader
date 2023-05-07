package com.fox2code.foxloader.commands;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.GameRegistry;
import com.fox2code.foxloader.registry.RegisteredWorld;

public class WorldReplace extends CommandCompat {
    public WorldReplace() {
        super("/replace");
    }

    @Override
    public void onExecute(String[] args, NetworkPlayer commandExecutor) {
        GameRegistry gameRegistry = GameRegistry.getInstance();
        if (args.length != 3) {
            commandExecutor.displayChatMessage(gameRegistry.translateKey("command.usage.id-meta-twice"));
            return;
        }
        if (!commandExecutor.getNetworkPlayerController().hasCreativeModeRegistered()) {
            commandExecutor.displayChatMessage(
                    gameRegistry.translateKey("command.error.creative-only"));
            return;
        }

        String arg = args[1];
        int i = arg.indexOf(':');
        int idSource, metaSource;
        if (i == -1) {
            idSource = Integer.parseInt(arg);
            metaSource = -1;
        } else {
            idSource = Integer.parseInt(arg.substring(0, i));
            metaSource = Integer.parseInt(arg.substring(i + 1));
        }
        if (idSource < 0 || idSource > GameRegistry.getInstance().getMaxBlockId()) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.invalid-block-id", Integer.toString(idSource)));
            return;
        }
        if (i != -1 && (metaSource < 0 || metaSource > 15)) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.invalid-meta-data", Integer.toString(metaSource)));
            return;
        }
        arg = args[2];
        i = arg.indexOf(':');
        int idTarget, metaTarget;
        if (i == -1) {
            idTarget = Integer.parseInt(arg);
            metaTarget = 0;
        } else {
            idTarget = Integer.parseInt(arg.substring(0, i));
            metaTarget = Integer.parseInt(arg.substring(i + 1));
        }
        if (idTarget < 0 || idTarget > GameRegistry.getInstance().getMaxBlockId()) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.invalid-block-id", Integer.toString(idTarget)));
            return;
        }
        if (metaTarget < 0 || metaTarget > 15) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.invalid-meta-data", Integer.toString(metaSource)));
            return;
        }

        NetworkPlayer.NetworkPlayerController networkPlayerController =
                commandExecutor.getNetworkPlayerController();
        if (!networkPlayerController.hasSelection()) {
            commandExecutor.displayChatMessage(
                    gameRegistry.translateKey("command.error.no-area-selected"));
            return;
        }
        final int minX = networkPlayerController.getMinX();
        final int maxX = networkPlayerController.getMaxX();
        final int minY = networkPlayerController.getMinY();
        final int maxY = networkPlayerController.getMaxY();
        final int minZ = networkPlayerController.getMinZ();
        final int maxZ = networkPlayerController.getMaxZ();
        final long blockChanged = (maxX - (long) minX) * (maxX - minX) * (maxX - minX);
        if (blockChanged > 1000000) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.changing-too-many-blocks", Long.toString(blockChanged)));
            return;
        }
        commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                "command.changing-blocks", Long.toString(blockChanged)));

        final RegisteredWorld world = commandExecutor.getCurrentRegisteredWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getRegisteredBlockId(x, y, z) == idSource && (metaSource == -1 ||
                            world.getRegisteredBlockMetadata(x, y, z) == metaSource)) {
                        world.setRegisteredBlockAndMetadataWithNotify(x, y, z, idTarget, metaTarget);
                    }
                }
            }
        }

        commandExecutor.displayChatMessage(gameRegistry.translateKey("command.done"));
    }
}
