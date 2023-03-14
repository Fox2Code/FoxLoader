package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.server.network.NetServerHandlerAccessor;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.server.packets.NetServerHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NetServerHandler.class)
public class MixinNetServerHandler implements NetServerHandlerAccessor {
    @Shadow private EntityPlayerMP playerEntity;

    @Override
    public EntityPlayerMP getPlayerEntity() {
        return this.playerEntity;
    }
}
