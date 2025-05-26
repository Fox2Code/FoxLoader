package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredBlockAccess;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.level.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IBlockAccess.class)
public interface MixinIBlockAccess extends RegisteredBlockAccess<Block> {
	@Shadow int getBlockId(int x, int y, int z);

	@Override
	default Block getBlock(int x, int y, int z) {
		return Block.blocksList[this.getBlockId(x, y, z)];
	}
}
