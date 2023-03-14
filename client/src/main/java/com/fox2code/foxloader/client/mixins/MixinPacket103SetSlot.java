package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.network.io.NetworkDataInputStream;
import com.fox2code.foxloader.registry.GameRegistryClient;
import net.minecraft.src.client.packets.Packet103SetSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.DataInputStream;
import java.io.IOException;

@Mixin(Packet103SetSlot.class)
public class MixinPacket103SetSlot {
    @Redirect(method = "readPacketData", at = @At(value = "INVOKE",
            target = "Ljava/io/DataInputStream;readShort()S", ordinal = 1))
    public short readItemId(DataInputStream instance) throws IOException {
        short itemId = instance.readShort();
        if (itemId >= 0 && instance instanceof NetworkDataInputStream) {
            itemId = GameRegistryClient.itemIdMappingIn[itemId];
        }
        return itemId;
    }
}
