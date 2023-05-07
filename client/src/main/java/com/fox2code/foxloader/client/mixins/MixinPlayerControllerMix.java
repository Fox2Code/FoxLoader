package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.ImplNetworkPlayerControllerExt;
import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.player.PlayerController;
import net.minecraft.src.client.player.PlayerControllerMP;
import net.minecraft.src.client.player.PlayerControllerSP;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {PlayerControllerMP.class, PlayerControllerSP.class})
public class MixinPlayerControllerMix extends PlayerController implements
        NetworkPlayer.NetworkPlayerController, ImplNetworkPlayerControllerExt {
    @Unique int x1, x2, y1, y2, z1, z2;
    @Unique boolean hasPrimary, hasSecondary;

    public MixinPlayerControllerMix(Minecraft minecraft) {
        super(minecraft);
    }

    @Inject(method = "clickBlock", at = @At("HEAD"), cancellable = true)
    public void onBlockClicked(int x, int y, int z, int facing, CallbackInfo ci) {
        ItemStack item = this.mc.thePlayer.getCurrentEquippedItem();
        NetworkPlayer networkPlayer = (NetworkPlayer) this.mc.thePlayer;
        if (ModLoader.Internal.notifyPlayerStartBreakBlock(networkPlayer,
                ClientMod.toRegisteredItemStack(item), x, y, z, facing)) {
            ci.cancel();
            return;
        }
        if ((!this.mc.theWorld.multiplayerWorld) && item != null &&
                item.itemID == Item.axeWood.itemID && this.isInCreativeMode() &&
                networkPlayer.isOperator()) {
            x1 = x;
            y1 = y;
            z1 = z;
            hasPrimary = true;
            NetworkPlayer.NetworkPlayerController controller = networkPlayer.getNetworkPlayerController();
            if (controller != this && controller instanceof ImplNetworkPlayerControllerExt) {
                ((ImplNetworkPlayerControllerExt) controller).notifyRegisteredSetPrimary(x, y, z);
            }
            networkPlayer.displayChatMessage(
                    "Pos1: [" + x + ", " + y + ", " + z + "]");
            ci.cancel();
        }
    }

    @Inject(method = "sendBlockRemoved", at = @At("HEAD"), cancellable = true)
    public void onBlockRemoving(int x, int y, int z, int facing, CallbackInfoReturnable<Boolean> cir) {
        ItemStack item = this.mc.thePlayer.getCurrentEquippedItem();
        if (ModLoader.Internal.notifyPlayerBreakBlock(ClientMod.getLocalNetworkPlayer(),
                ClientMod.toRegisteredItemStack(item), x, y, z, facing)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Override
    public boolean notifyRegisteredItemUsedImpl(EntityPlayer player, ItemStack itemstack, int x, int y, int z) {
        NetworkPlayer networkPlayer = (NetworkPlayer) this.mc.thePlayer;
        if ((!this.mc.theWorld.multiplayerWorld) && itemstack != null &&
                itemstack.itemID == Item.axeWood.itemID && this.isInCreativeMode() &&
                networkPlayer.isOperator()) {
            x2 = x;
            y2 = y;
            z2 = z;
            hasSecondary = true;
            NetworkPlayer.NetworkPlayerController controller = networkPlayer.getNetworkPlayerController();
            if (controller != this && controller instanceof ImplNetworkPlayerControllerExt) {
                ((ImplNetworkPlayerControllerExt) controller).notifyRegisteredSetSecondary(x, y, z);
            }
            networkPlayer.displayChatMessage(
                    "Pos2: [" + x + ", " + y + ", " + z + "]");
            return true;
        }
        return false;
    }

    @Override
    public void notifyRegisteredSetPrimary(int x, int y, int z) {
        this.x1 = x; this.y1 = y; this.z1 = z; this.hasPrimary = true;
    }

    @Override
    public void notifyRegisteredSetSecondary(int x, int y, int z) {
        this.x2 = x; this.y2 = y; this.z2 = z; this.hasSecondary = true;
    }

    @Override
    public boolean hasSelection() {
        return hasPrimary && hasSecondary;
    }

    @Override
    public int getMinX() {
        return Math.min(x1, x2);
    }

    @Override
    public int getMaxX() {
        return Math.max(x1, x2);
    }

    @Override
    public int getMinY() {
        return Math.min(y1, y2);
    }

    @Override
    public int getMaxY() {
        return Math.max(y1, y2);
    }

    @Override
    public int getMinZ() {
        return Math.min(z1, z2);
    }

    @Override
    public int getMaxZ() {
        return Math.max(z1, z2);
    }
}
