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
