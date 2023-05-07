package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.ImplNetworkPlayerControllerExt;
import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.player.PlayerController;
import net.minecraft.src.client.player.PlayerControllerSP;
import net.minecraft.src.client.renderer.Vec3D;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerControllerSP.class)
public class MixinPlayerControllerSP extends PlayerController implements ImplNetworkPlayerControllerExt {
    public MixinPlayerControllerSP(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean sendUseItem(EntityPlayer player, World var2, ItemStack var3) {
        if (ModLoader.Internal.notifyPlayerUseItem((NetworkPlayer) player,
                ClientMod.toRegisteredItemStack(var3))) {
            return false;
        }
        return super.sendUseItem(player, var2, var3);
    }

    @Override
    public boolean sendPlaceBlock(EntityPlayer player, World world, ItemStack itemstack, int x, int y, int z, int facing, Vec3D vec3d) {
        if (ModLoader.Internal.notifyPlayerUseItemOnBlock((NetworkPlayer) player,
                ClientMod.toRegisteredItemStack(itemstack), x, y, z, facing,
                (float) vec3d.xCoord,(float) vec3d.yCoord,(float) vec3d.zCoord)) {
            return false;
        }
        if (this.notifyRegisteredItemUsedImpl(player, itemstack, x, y, z)) {
            return false;
        }
        return super.sendPlaceBlock(player, world, itemstack, x, y, z, facing, vec3d);
    }
}
