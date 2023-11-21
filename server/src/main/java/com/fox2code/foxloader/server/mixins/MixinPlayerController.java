package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ServerMod;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.src.game.MathHelper;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.entity.player.EntityPlayerMP;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.level.World;
import net.minecraft.src.server.packets.Packet53BlockChange;
import net.minecraft.src.server.player.PlayerController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerController.class)
public abstract class MixinPlayerController implements NetworkPlayer.NetworkPlayerController {
    @Unique int x1, x2, y1, y2, z1, z2;
    @Unique boolean hasPrimary, hasSecondary;
    @Unique int lastBreakFacing;

    @Shadow public abstract boolean isCreative();

    @Shadow public EntityPlayer player;

    @Shadow public World worldObj;

    @Inject(method = "blockClicked", at = @At("HEAD"), cancellable = true)
    public void onBlockClicked(int x, int y, int z, int facing, CallbackInfo ci) {
        this.lastBreakFacing = facing;
        ItemStack item = this.player.getCurrentEquippedItem();
        NetworkPlayer networkPlayer = (NetworkPlayer) this.player;
        if (ModLoader.Internal.notifyPlayerStartBreakBlock(networkPlayer,
                ServerMod.toRegisteredItemStack(item), x, y, z, facing)) {
            this.sendBlockUpdateFacingHelper(x, y, z, facing);
            ci.cancel();
            return;
        }
        if (this.isCreative() && networkPlayer.isOperator() &&
                item != null && item.itemID == Item.axeWood.itemID) {
            ((EntityPlayerMP)this.player).playerNetServerHandler.sendPacket(
                    new Packet53BlockChange(x, y, z, this.worldObj)
            );
            x1 = x;
            y1 = y;
            z1 = z;
            hasPrimary = true;
            networkPlayer.displayChatMessage(
                    "Pos1: [" + x + ", " + y + ", " + z + "]");
            ci.cancel();
        }
    }

    @Inject(method = "blockRemoving", at = @At("HEAD"), cancellable = true)
    public void onBlockRemoving(int x, int y, int z, CallbackInfo ci) {
        ItemStack item = this.player.getCurrentEquippedItem();
        NetworkPlayer networkPlayer = (NetworkPlayer) this.player;
        if (ModLoader.Internal.notifyPlayerBreakBlock(networkPlayer,
                ServerMod.toRegisteredItemStack(item), x, y, z, this.lastBreakFacing)) {
            Packet53BlockChange packet53BlockChange =
                    new Packet53BlockChange(x, y, z, this.worldObj);
            packet53BlockChange.metadata = 0;
            packet53BlockChange.type = (short) Block.bedrock.blockID;
            ((EntityPlayerMP)this.player).playerNetServerHandler.sendPacket(packet53BlockChange);
            ((EntityPlayerMP)this.player).playerNetServerHandler.sendPacket(
                    new Packet53BlockChange(x, y, z, this.worldObj)
            );
            ci.cancel();
        }
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    public void onItemUsed(EntityPlayer entityplayer, World world, ItemStack itemstack, CallbackInfoReturnable<Boolean> cir) {
        if (ModLoader.Internal.notifyPlayerUseItem((NetworkPlayer) player,
                ServerMod.toRegisteredItemStack(itemstack))) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "activeBlockOrUseItem", at = @At("HEAD"), cancellable = true)
    public void onActiveBlockOrUseItem(EntityPlayer player, World world, ItemStack itemstack, int x, int y, int z,
                                       int facing, float xVec, float yVec, float zVec, CallbackInfoReturnable<Boolean> cir) {
        ItemStack item = this.player.getCurrentEquippedItem();
        NetworkPlayer networkPlayer = (NetworkPlayer) this.player;
        if (ModLoader.Internal.notifyPlayerUseItemOnBlock((NetworkPlayer) player,
                ServerMod.toRegisteredItemStack(itemstack), x, y, z, facing, xVec, yVec, zVec)) {
            this.sendBlockUpdateFacingHelper(x, y, z, facing);
            cir.setReturnValue(Boolean.FALSE);
            return;
        }

        if (this.isCreative() && networkPlayer.isOperator() &&
                item != null && item.itemID == Item.axeWood.itemID) {
            ((EntityPlayerMP)this.player).playerNetServerHandler.sendPacket(
                    new Packet53BlockChange(x, y, z, this.worldObj)
            );
            x2 = x;
            y2 = y;
            z2 = z;
            hasSecondary = true;
            networkPlayer.displayChatMessage(
                    "Pos2: [" + x + ", " + y + ", " + z + "]");
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Unique
    private void sendBlockUpdateFacingHelper(int x, int y, int z, int facing) {
        ((EntityPlayerMP)this.player).playerNetServerHandler.sendPacket(
                new Packet53BlockChange(x, y, z, this.worldObj)
        );
        int rot = MathHelper.floor_double((double)(player.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        byte xoffs = 0;
        byte zoffs = 0;
        int ignoreFacing;
        switch (rot) {
            default:
                return;
            case 0:
                zoffs = 1;
                ignoreFacing = 3;
                break;
            case 1:
                xoffs = -1;
                ignoreFacing = 4;
                break;
            case 2:
                zoffs = -1;
                ignoreFacing = 2;
                break;
            case 3:
                xoffs = 1;
                ignoreFacing = 5;
                break;
        }
        ((EntityPlayerMP) this.player).playerNetServerHandler.sendPacket(
                new Packet53BlockChange(x + xoffs, y, z + zoffs, this.worldObj)
        );
        if (facing == ignoreFacing) return;
        switch(facing) {
            case 0:
                --y;
                break;
            case 1:
                ++y;
                break;
            case 2:
                --z;
                break;
            case 3:
                ++z;
                break;
            case 4:
                --x;
                break;
            case 5:
                ++x;
                break;
            default:
                return;
        }
        ((EntityPlayerMP)this.player).playerNetServerHandler.sendPacket(
                new Packet53BlockChange(x, y, z, this.worldObj)
        );
    }

    @Override
    public boolean hasCreativeModeRegistered() {
        return this.isCreative();
    }

    @Override
    public boolean hasSelection() {
        return hasPrimary && hasSecondary;
    }

    public int getMinX() {
        return Math.min(x1, x2);
    }

    public int getMaxX() {
        return Math.max(x1, x2);
    }

    public int getMinY() {
        return Math.min(y1, y2);
    }

    public int getMaxY() {
        return Math.max(y1, y2);
    }

    public int getMinZ() {
        return Math.min(z1, z2);
    }

    public int getMaxZ() {
        return Math.max(z1, z2);
    }
}
