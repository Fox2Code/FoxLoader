package com.fox2code.foxloader.registry.missing;

import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.block.ItemBlock;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.world.World;

public class MissingItemBlock extends ItemBlock {
    public MissingItemBlock(MissingBlock block) {
        super(block);
        this.setMaxStackSize(1);
        this.hideFromCreativeMenu();
    }

    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer player, World world,
                             int x, int y, int z, int facing, float xVec, float yVec, float zVec) {
        if (world.isRemote || world.isServer || !player.capabilities.isCreativeMode) {
            return false;
        }
        return super.onItemUse(itemstack, player, world, x, y, z, facing, xVec, yVec, zVec);
    }
}
