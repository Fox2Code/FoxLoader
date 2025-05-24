package com.fox2code.foxloader.client.utils;

import com.fox2code.foxloader.utils.BlockFace;
import com.fox2code.foxloader.utils.BlockWithFaces;
import net.minecraft.src.client.renderer.block.icon.Icon;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.level.IBlockAccess;
import net.minecraft.src.game.level.World;

/**
 * Implemented in {@link Block}.
 */
public interface ClientBlockWithFaces extends BlockWithFaces<IBlockAccess, World> {
	/**
	 * Only implemented on the client.
	 */
	default void setIcon(Icon icon, BlockFace blockFace, int metadata) {
	}

	/**
	 * Only implemented on client
	 */
	default Icon getIcon(BlockFace blockFace) {
		return this.getIcon(blockFace, 0);
	}

	/**
	 * Only implemented on the client.
	 */
	default Icon getIcon(BlockFace blockFace, int metadata) {
		return null;
	}

	// Similar names that should not be used

	/**
	 * Only implemented on the client.
	 */
	default void setTexture(Icon icon, BlockFace blockFace, int metadata) {
		setIcon(icon, blockFace, metadata);
	}

	/**
	 * Only implemented on the client.
	 */
	default Icon getBlockTextureFromSide(BlockFace blockFace) {
		return this.getIcon(blockFace);
	}

	/**
	 * Only implemented on the client.
	 */
	default Icon getTexture(BlockFace blockFace, int metadata) {
		return this.getIcon(blockFace, metadata);
	}
}
