package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.loader.ModLoaderOptions;
import net.minecraft.src.client.renderer.ItemRenderer;
import net.minecraft.src.game.entity.EntityLiving;
import net.minecraft.src.game.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    @Inject(method = "renderItem", at = @At(value = "INVOKE", target =
            "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onItemPreRenderBlock(EntityLiving entity, ItemStack item, float deltaTicks, CallbackInfo ci) {
        ClientModLoader.Internal.glScaleItem(item);
    }

    @Inject(method = "renderItem", at = @At(value = "INVOKE", target =
            "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onItemPreRender(EntityLiving entity, ItemStack item, float deltaTicks, CallbackInfo ci) {
        ClientModLoader.Internal.glScaleItemOverXYTranslate(item, -0.0625F, 0.125f);
    }

    @Inject(method = "renderItemAtCoords", at = @At(value = "INVOKE", target =
            "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onItemPreRenderCoordBlock(int xCoord, int yCoord, int zCoord, ItemStack item, float light, CallbackInfo ci) {
        ClientModLoader.Internal.glScaleItemNoZFighting(item);
    }

    @Inject(method = "renderItemAtCoords", at = @At(value = "INVOKE", target =
            "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onItemPreRenderCoord(int xCoord, int yCoord, int zCoord, ItemStack item, float light, CallbackInfo ci) {
        ClientModLoader.Internal.glScaleItemOverZTranslate(item, 0.3125F);
    }
}
