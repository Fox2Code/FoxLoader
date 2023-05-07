package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.player.PlayerController;
import net.minecraft.src.client.player.PlayerControllerMP;
import net.minecraft.src.client.renderer.Vec3D;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP extends PlayerController {
    public MixinPlayerControllerMP(Minecraft minecraft) {
        super(minecraft);
    }

    @Inject(method = "sendUseItem", at = @At("HEAD"), cancellable = true)
    public void onSendUseItem(EntityPlayer player, World world,
                              ItemStack itemstack, CallbackInfoReturnable<Boolean> cir) {
        if (ModLoader.Internal.notifyPlayerUseItem((NetworkPlayer) player,
                ClientMod.toRegisteredItemStack(itemstack))) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "sendPlaceBlock", at = @At("HEAD"), cancellable = true)
    public void onSendPlaceBlock(EntityPlayer player, World world, ItemStack itemstack,
                                int x, int y, int z, int facing, Vec3D vec3d, CallbackInfoReturnable<Boolean> cir) {
        if (ModLoader.Internal.notifyPlayerUseItemOnBlock((NetworkPlayer) player,
                ClientMod.toRegisteredItemStack(itemstack), x, y, z, facing,
                (float) vec3d.xCoord,(float) vec3d.yCoord,(float) vec3d.zCoord)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

}
