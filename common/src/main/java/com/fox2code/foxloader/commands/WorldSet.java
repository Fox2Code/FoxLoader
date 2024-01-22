package com.fox2code.foxloader.commands;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.GameRegistry;
import com.fox2code.foxloader.registry.RegisteredWorld;

public class WorldSet extends CommandCompat {
    public WorldSet() {
        super("/set");
    }

    @Override
    public void onExecute(String[] args, NetworkPlayer commandExecutor) {
        GameRegistry gameRegistry = GameRegistry.getInstance();
        if (args.length != 2) {
            commandExecutor.displayChatMessage(gameRegistry.translateKey("command.usage.id-meta"));
            return;
        }
        if (!commandExecutor.getNetworkPlayerController().hasCreativeModeRegistered()) {
            commandExecutor.displayChatMessage(
                    gameRegistry.translateKey("command.error.creative-only"));
            return;
        }

        String arg = args[1];
        int i = arg.indexOf(':');
        int id, meta;
        if (i == -1) {
            id = Integer.parseInt(arg);
            meta = 0;
        } else {
            id = Integer.parseInt(arg.substring(0, i));
            meta = Integer.parseInt(arg.substring(i + 1));
        }
        if (id < 0 || id > GameRegistry.getInstance().getMaxBlockId()) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.invalid-block-id", Integer.toString(id)));
            return;
        }
        if (meta < 0 || meta > 15) {
            commandExecutor.displayChatMessage(gameRegistry.translateKeyFormat(
                    "command.error.invalid-meta-data", Integer.toString(meta)));
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
        final long blockChanged = ((maxX - (long) minX) + 1) * ((maxY - minY) + 1) * ((maxZ - minZ) + 1);
        if (blockChanged > 1000000000) {
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
                    world.setRegisteredBlockAndMetadataWithNotify(x, y, z, id, meta);
                }
            }
        }

        commandExecutor.displayChatMessage(gameRegistry.translateKey("command.done"));
    }
}
