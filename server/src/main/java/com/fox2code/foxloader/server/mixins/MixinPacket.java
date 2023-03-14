package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.server.network.Packet250PluginMessage;
import net.minecraft.src.server.packets.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Packet.class)
public abstract class MixinPacket {
    @Shadow static void addIdClassMapping(int var0, boolean var1, boolean var2, Class<? extends Packet> var3) {}

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void initHook(CallbackInfo ci) {
        addIdClassMapping(250, true, true, Packet250PluginMessage.class);
    }
}
