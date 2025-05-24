package com.fox2code.foxloader.utils;

/**
 * Implemented in Block.
 * @param <BA> IBlockAccess type
 * @param <W> World type
 */
public interface BlockWithFaces<BA, W> {
	// Overloads

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
