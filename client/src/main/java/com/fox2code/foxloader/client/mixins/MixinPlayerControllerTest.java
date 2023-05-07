package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.ImplNetworkPlayerControllerExt;
import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.player.PlayerController;
import net.minecraft.src.client.player.PlayerControllerTest;
import net.minecraft.src.client.renderer.Vec3D;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerTest.class)
public abstract class MixinPlayerControllerTest extends PlayerController implements
        NetworkPlayer.NetworkPlayerController, ImplNetworkPlayerControllerExt {
    @Unique int x1, x2, y1, y2, z1, z2;
    @Unique boolean hasPrimary, hasSecondary;

    public MixinPlayerControllerTest(Minecraft minecraft) {
        super(minecraft);
    }

    @Shadow public abstract boolean isInCreativeMode();

    @Override
    public void clickBlock(int x, int y, int z, int facing) {
        ItemStack item = this.mc.thePlayer.getCurrentEquippedItem();
        NetworkPlayer networkPlayer = (NetworkPlayer) this.mc.thePlayer;
        if (ModLoader.Internal.notifyPlayerStartBreakBlock(networkPlayer,
                ClientMod.toRegisteredItemStack(item), x, y, z, facing)) {
            return;
        }
        if ((!this.mc.theWorld.multiplayerWorld) && item != null &&
                item.itemID == Item.axeWood.itemID && this.isInCreativeMode() &&
                networkPlayer.isOperator()) {
            x1 = x;
            y1 = y;
            z1 = z;
            hasPrimary = true;
            networkPlayer.displayChatMessage(
                    "Pos1: [" + x + ", " + y + ", " + z + "]");
            return;
        }
        super.clickBlock(x, y, z, facing);
    }

    @Override
    public boolean sendBlockRemoved(int x, int y, int z, int facing) {
        ItemStack item = this.mc.thePlayer.getCurrentEquippedItem();
        if (ModLoader.Internal.notifyPlayerBreakBlock(ClientMod.getLocalNetworkPlayer(),
                ClientMod.toRegisteredItemStack(item), x, y, z, facing)) {
            return false;
        }
        return super.sendBlockRemoved(x, y, z, facing);
    }

    @Override
    public boolean sendUseItem(EntityPlayer player, World var2, ItemStack var3) {
        if (ModLoader.Internal.notifyPlayerUseItem((NetworkPlayer) player,
                ClientMod.toRegisteredItemStack(var3))) {
            return false;
        }
        return super.sendUseItem(player, var2, var3);
    }

    @Inject(method = "sendPlaceBlock", at = @At("HEAD"), cancellable = true)
    public void onSendPlaceBlock(EntityPlayer player, World world, ItemStack itemstack,
                                 int x, int y, int z, int facing, Vec3D vec3d, CallbackInfoReturnable<Boolean> cir) {
        NetworkPlayer networkPlayer = (NetworkPlayer) player;
        if (ModLoader.Internal.notifyPlayerUseItemOnBlock(networkPlayer,
                ClientMod.toRegisteredItemStack(itemstack), x, y, z, facing,
                (float) vec3d.xCoord,(float) vec3d.yCoord,(float) vec3d.zCoord)) {
            cir.setReturnValue(Boolean.FALSE);
        }
        if ((!this.mc.theWorld.multiplayerWorld) && itemstack != null &&
                itemstack.itemID == Item.axeWood.itemID && this.isInCreativeMode() &&
                networkPlayer.isOperator()) {
            x2 = x;
            y2 = y;
            z2 = z;
            hasSecondary = true;
            NetworkPlayer.NetworkPlayerController controller = networkPlayer.getNetworkPlayerController();
            if (controller != this && controller instanceof ImplNetworkPlayerControllerExt) {
                ((ImplNetworkPlayerControllerExt) controller).notifyRegisteredSetPrimary(x, y, z);
            }
            networkPlayer.displayChatMessage(
                    "Pos2: [" + x + ", " + y + ", " + z + "]");
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
