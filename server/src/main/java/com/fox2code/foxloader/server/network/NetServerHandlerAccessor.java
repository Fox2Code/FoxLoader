package com.fox2code.foxloader.server.network;

import net.minecraft.src.game.entity.player.EntityPlayerMP;

public interface NetServerHandlerAccessor {
    EntityPlayerMP getPlayerEntity();
}
