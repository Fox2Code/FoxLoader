/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
