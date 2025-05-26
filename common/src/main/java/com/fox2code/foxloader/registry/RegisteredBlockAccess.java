package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.utils.BlockFace;

/**
 * Interface implemented by sided mixins.
 * @param <B> Block type
 */
public interface RegisteredBlockAccess<B> {
	/**
	 * @return a block at the world coordinates provided
	 */
	B getBlock(int x, int y, int z);

	/**
	 * @return a block to the relative side of the block face at the provided coordinates
	 */
	default B getRelative(int x, int y, int z, BlockFace blockFace) {
		return getBlock(x + blockFace.directionX, y + blockFace.directionY, z + blockFace.directionZ);
	}
}
