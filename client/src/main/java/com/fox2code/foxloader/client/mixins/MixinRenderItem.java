package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.registry.GameRegistry;
import net.minecraft.src.client.renderer.RenderBlocks;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.entity.other.EntityItem;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import net.minecraft.src.client.renderer.entity.Render;
import net.minecraft.src.client.renderer.entity.RenderItem;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemRecord;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
abstract class MixinRenderItem extends Render {
	@Inject(method = "doRenderItem", at = @At(value = "INVOKE", target =
			"Lorg/lwjgl/opengl/GL11;glPushMatrix()V", ordinal = 1, shift = At.Shift.AFTER))
	private void onItemRenderBlock(
			EntityItem entityItem, double x, double y, double z, float yaw, float deltaTicks, CallbackInfo ci) {
		ClientModLoader.Internal.glScaleItem(entityItem.item);
	}

	@Inject(method = "renderItemEntity", at = @At(value = "INVOKE", target =
			"Lorg/lwjgl/opengl/GL11;glPushMatrix()V", ordinal = 0, shift = At.Shift.AFTER))
	private void onItemPreRender(
			EntityItem entityItem, ItemStack itemstack,
			float rotX, float rotY, float rotZ, float deltaTicks, CallbackInfo ci) {
		ClientModLoader.Internal.glScaleItem(itemstack);
	}

	@Redirect(method = "renderItemEntity", at = @At(value = "CONSTANT", args = {"classValue=net/minecraft/src/game/item/ItemRecord"}))
	private boolean accountForModBlocks(Object item, Class<ItemRecord> type) {
		return !GameRegistry.isLoaderReservedBlockItemId(((Item) item).itemID);
	}
}