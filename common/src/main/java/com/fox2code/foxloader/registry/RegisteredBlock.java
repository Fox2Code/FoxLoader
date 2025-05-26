package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.utils.BlockFace;

/**
 * Interface implemented by sided mixins.
 *
 */
public interface RegisteredBlock<BA, W> extends GameRegistry.Ingredient {
    default RegisteredItem asRegisteredItem() { throw new RuntimeException(); }

    default RegisteredItemStack newRegisteredItemStack() { throw new RuntimeException(); }

    default int getRegisteredBlockId() { throw new RuntimeException(); }

    // BlockFace overloads

    boolean shouldSideBeRendered(BA blockAccess, int x, int y, int z, BlockFace blockFace);

    boolean canPlaceBlockOnSide(W world, int x, int y, int z, BlockFace blockFace);

    void onBlockPlaced(W world, int x, int y, int z, BlockFace blockFace);

    boolean isPoweringTo(BA blockAccess, int x, int y, int z, BlockFace blockFace);

    boolean isIndirectlyPoweringTo(W world, int x, int y, int z, BlockFace blockFace);

    default int onBlockPlacedWithOffset(W world, int x, int y, int z, BlockFace blockFace,
                                        float xVec, float yVec, float zVec, int defaultReturn) {
        return defaultReturn;
    }
}
