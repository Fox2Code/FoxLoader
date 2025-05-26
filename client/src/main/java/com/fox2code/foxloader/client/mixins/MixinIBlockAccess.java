package com.fox2code.foxloader.client.mixins;

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
		int blockId = this.getBlockId(x, y, z);
		return Block.blocksList[blockId];
	}
}
