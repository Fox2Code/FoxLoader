package com.fox2code.foxloader.server.utils;

import com.fox2code.foxloader.utils.BlockWithFaces;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.level.IBlockAccess;
import net.minecraft.src.game.level.World;

/**
 * Implemented in {@link Block}.
 */
public interface ServerBlockWithFaces extends BlockWithFaces<IBlockAccess, World> {
}
