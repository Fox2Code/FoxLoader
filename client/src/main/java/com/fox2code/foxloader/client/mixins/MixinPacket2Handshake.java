package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.client.packets.Packet2Handshake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Packet2Handshake.class)
public class MixinPacket2Handshake {
    @Shadow public String username;

    @Inject(at = @At("RETURN"), method = "<init>(Ljava/lang/String;)V")
    public void onInit(String var1, CallbackInfo ci) {
        if (!this.username.endsWith(ModLoader.FOX_LOADER_HEADER)) {
            this.username += ModLoader.FOX_LOADER_HEADER;
        }
    }
}
