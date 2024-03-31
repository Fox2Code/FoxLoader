package com.fox2code.foxloader.client.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.src.client.renderer.entity.Render;
import net.minecraft.src.client.renderer.entity.RenderItem;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemRecord;

import com.fox2code.foxloader.registry.GameRegistry;

@Mixin(RenderItem.class)
abstract class MixinRenderItem extends Render {
	@Redirect(method = "renderItemEntity", at = @At(value = "CONSTANT", args = {"classValue=net/minecraft/src/game/item/ItemRecord"}))
	private boolean accountForModBlocks(Object item, Class<ItemRecord> type) {
		int block = GameRegistry.convertItemIdToBlockId(((Item) item).itemID);
		return block < 0 || Block.blocksList[block].blockID != block;
	}
}