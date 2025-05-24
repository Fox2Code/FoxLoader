package com.fox2code.foxloader.server.mixins;

import net.minecraft.src.game.block.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TileEntity.class)
public interface AccessorTileEntity {
	@Invoker
	static void invokeAddMapping(Class<?> entityClass, String entityTypeName) {
		throw new IllegalStateException();
	}
}
