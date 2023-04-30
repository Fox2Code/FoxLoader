package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.NetClientHandlerExtensions;
import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.packets.*;
import net.minecraft.src.game.level.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetClientHandler.class)
public class MixinNetClientHandler implements NetClientHandlerExtensions {
    boolean isFoxLoader = false;

    @Inject(method = "handlePickupSpawn", at = @At("HEAD"))
    public void onHandlePickupSpawn(Packet21PickupSpawn packet21, CallbackInfo ci) {
        if (packet21.itemID >= 0) {
            packet21.itemID = GameRegistryClient.itemIdMappingIn[packet21.itemID];
        }
    }

    @Inject(method = "handleVehicleSpawn", at = @At("HEAD"))
    public void onHandleVehicleSpawn(Packet23VehicleSpawn packet23, CallbackInfo ci) {
        if (packet23.type == 70) {
            packet23.payload0 = GameRegistryClient.itemIdMappingIn[packet23.payload0];
        }
    }

    @Inject(method = "handleMultiBlockChange", at = @At("HEAD"))
    public void onHandleMultiBlockChange(Packet52MultiBlockChange packet52, CallbackInfo ci) {
        short[] types = packet52.typeArray;
        final int len = types.length;
        for (int i = 0; i < len; i++) {
            types[i] = GameRegistryClient.blockIdMappingIn[types[i]];
        }
    }

    @Redirect(method = "handleMapChunk", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/level/WorldClient;setChunkData(IIIIII[B)V"))
    public void onSetChunkData(WorldClient instance, int mx, int my, int mz, int mxx, int mxy, int mxz, byte[] data) {
        ClientModLoader.Internal.networkChunkBytes = data;
        try {
            instance.setChunkData(mx, my, mz, mxx, mxy, mxz, data);
        } finally {
            ClientModLoader.Internal.networkChunkBytes = null;
        }
    }

    @Inject(method = "handleBlockChange", at = @At("HEAD"))
    public void onHandleBlockChange(Packet53BlockChange packet53, CallbackInfo ci) {
        packet53.type = GameRegistryClient.blockIdMappingIn[packet53.type];
    }

    @Inject(method = "handlePlayerInventory", at = @At("HEAD"))
    public void onHandlePickupSpawn(Packet5PlayerInventory var1, CallbackInfo ci) {
        if (var1.itemID >= 0) {
            var1.itemID = GameRegistryClient.itemIdMappingIn[var1.itemID];
        }
    }

    @Inject(method = "handlePluginMessage", at = @At("HEAD"))
    public void onHandlePluginMessage(Packet250PluginMessage packet250, CallbackInfo ci) {
        NetworkPlayer networkPlayer = (NetworkPlayer)
                Minecraft.getInstance().thePlayer;
        if (ModLoader.FOX_LOADER_MOD_ID.equals(packet250.channel)) {
            this.isFoxLoader = true;
        }
        ModContainer modContainer = ModLoader.getModContainer(packet250.channel);
        if (networkPlayer != null && modContainer != null && packet250.data != null) {
            modContainer.notifyReceiveServerPacket(networkPlayer, packet250.data);
        }
    }

    @Override
    public boolean isFoxLoader() {
        return this.isFoxLoader;
    }
}
