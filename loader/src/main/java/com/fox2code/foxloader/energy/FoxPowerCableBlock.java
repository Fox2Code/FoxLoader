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
