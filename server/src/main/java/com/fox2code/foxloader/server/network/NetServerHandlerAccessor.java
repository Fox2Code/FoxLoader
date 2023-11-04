package com.fox2code.foxloader.server.network;

import net.minecraft.src.game.entity.player.EntityPlayerMP;

public interface NetServerHandlerAccessor {
    EntityPlayerMP getPlayerEntity();
    void notifyHasFoxLoader();
    boolean hasFoxLoader();
    void notifyClientHello();
    boolean hasClientHello();
}
