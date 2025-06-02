package com.fox2code.foxloader.energy;

import net.minecraft.common.world.BlockAccess;
import org.jetbrains.annotations.Nullable;

public interface FoxPowerBlock {
    /**
     * Get the FoxPowerInterface for the face of a block.
     *
     * @param blockAccess the block access
     * @param x the x block pos coordinate
     * @param y the y block pos coordinate
     * @param z the z block pos coordinate
     * @param blockFace the blockFace to get a FoxPowerInterface from
     * @return the {@link FoxPowerInterface} for this block
     */
    @Nullable default FoxPowerInterface getPowerInterfaceForFace(BlockAccess blockAccess, int x, int y, int z, int blockFace) {
        return this.getIntrinsicPowerInterface(blockAccess, x, y, z);
    }

    /**
     * Get the FoxPowerInterface for a block, can be used to get the true energy amount of a block.
     *
     * @param blockAccess the block access
     * @param x the x block pos coordinate
     * @param y the y block pos coordinate
     * @param z the z block pos coordinate
     * @return the {@link FoxPowerInterface} for this block
     */
    @Nullable FoxPowerInterface getIntrinsicPowerInterface(BlockAccess blockAccess, int x, int y, int z);

    /**
     * Called to notify a block a new neighboring power block has updated its power interface.
     *
     * @param blockAccess the block access
     * @param x the x block pos coordinate
     * @param y the y block pos coordinate
     * @param z the z block pos coordinate
     * @param blockFace the blockFace to get a FoxPowerInterface from
     * @return if neighbors should be power updated.
     */
    default boolean notifyPowerInterfaceForFace(BlockAccess blockAccess, int x, int y, int z, int blockFace) {
        return false;
    }
}
