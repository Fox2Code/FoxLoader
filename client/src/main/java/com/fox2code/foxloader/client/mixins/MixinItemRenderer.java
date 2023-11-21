package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.renderer.TextureDynamic;
import net.minecraft.src.client.renderer.ItemRenderer;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    @Shadow private ItemStack itemToRender;

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), index = 2, argsOnly = true)
    public ItemStack modifyArgIndex2(ItemStack value) {
        return TextureDynamic.Hooks.toRenderItemStack(value);
    }

    @ModifyVariable(method = "renderItemAtCoords", at = @At("HEAD"), index = 4, argsOnly = true)
    public ItemStack modifyArgIndex4(ItemStack value) {
        return TextureDynamic.Hooks.toRenderItemStack(value);
    }

    @Inject(method = "updateEquippedItem", at = @At("RETURN"))
    public void updateEquippedItemHook(CallbackInfo ci) {
        this.itemToRender = TextureDynamic.Hooks.toRenderItemStack(this.itemToRender);
    }
}
