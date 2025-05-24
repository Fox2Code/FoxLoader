package com.fox2code.foxloader.utils;

/**
 * Implemented on IBlockAccess
 * @param <B> Block type
 */
public interface FaceAwareBlockAccess<B> {
	B getBlock(int x, int y, int z);
	B getRelative(int x, int y, int z, BlockFace blockFace);
}
