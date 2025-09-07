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
package com.fox2code.foxloader.energy;

import net.minecraft.common.block.children.BlockContainer;
import net.minecraft.common.block.data.Material;
import net.minecraft.common.block.tileentity.TileEntity;
import net.minecraft.common.util.Direction;
import net.minecraft.common.world.BlockAccess;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Generic cable implementation, using {@link FoxPowerCableTileEntity} as implementing tile entity.
 */
public abstract class FoxPowerCableBlock extends BlockContainer implements FoxPowerBlock {
    public FoxPowerCableBlock(String name, Material material) {
        super(name, material);
    }

    @Override
    public FoxPowerCableTileEntity getBlockEntity() {
        return new FoxPowerCableTileEntity(this);
    }

    @Override
    public @Nullable FoxPowerInterface getIntrinsicPowerInterface(BlockAccess blockAccess, int x, int y, int z) {
        TileEntity tileEntity = blockAccess.getBlockTileEntity(x, y, z);
        return tileEntity instanceof FoxPowerCableTileEntity ?
                ((FoxPowerCableTileEntity) tileEntity).powerInterface : null;
    }

    @Override
    public @Nullable FoxPowerInterface getPowerInterfaceForFace(BlockAccess blockAccess, int x, int y, int z, int blockFace) {
        TileEntity tileEntity = blockAccess.getBlockTileEntity(x, y, z);
        return tileEntity instanceof FoxPowerCableTileEntity ?
                ((FoxPowerCableTileEntity) tileEntity).getPowerInterfaceForFace(
                        Direction.EnumDirection.VALID_DIRECTIONS[blockFace]) : null;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        FoxPowerCableTileEntity foxPowerCableTileEntity = this.getBlockEntity();
        world.setBlockTileEntity(x, y, z, foxPowerCableTileEntity);
        foxPowerCableTileEntity.updateSinkPriorities();
    }

    @Override
    public boolean notifyPowerInterfaceForFace(BlockAccess blockAccess, int x, int y, int z, int blockFace) {
        TileEntity tileEntity = blockAccess.getBlockTileEntity(x, y, z);
        if (tileEntity instanceof FoxPowerCableTileEntity) {
            return ((FoxPowerCableTileEntity) tileEntity).updateSinkPriorityForFace(
                    Direction.EnumDirection.VALID_DIRECTIONS[blockFace]);
        }
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int arg5) {
        TileEntity tileEntity = world.getBlockTileEntityImmediate(x, y, z);
        if (tileEntity instanceof FoxPowerCableTileEntity) {
            ((FoxPowerCableTileEntity) tileEntity).updateSinkPriorities();
        }
    }

    public abstract long getCableThroughput();

    public abstract long getCableStorage();
}
