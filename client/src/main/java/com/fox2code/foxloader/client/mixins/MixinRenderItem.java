package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.renderer.TextureDynamic;
import net.minecraft.src.client.renderer.Tessellator;
import net.minecraft.src.client.renderer.block.icon.Icon;
import net.minecraft.src.client.renderer.entity.RenderItem;
import net.minecraft.src.game.entity.other.EntityItem;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderItem.class)
public class MixinRenderItem {
    @Shadow public float zLevel;

    @ModifyVariable(method = "renderItemEntity", at = @At("HEAD"), index = 2, argsOnly = true)
    public ItemStack modifyArgIndex2(ItemStack value) {
        return TextureDynamic.Hooks.toRenderItemStack(value);
    }

    @Redirect(method = "doRenderItem", at = @At(value = "FIELD", target =
            "Lnet/minecraft/src/game/entity/other/EntityItem;item:Lnet/minecraft/src/game/item/ItemStack;", ordinal = 0))
    public ItemStack doRenderItemHookRedirect(EntityItem instance) {
        return TextureDynamic.Hooks.toRenderItemStack(instance.item);
    }

    @ModifyVariable(method = "renderItemIntoGUI", at = @At("HEAD"), index = 3, argsOnly = true)
    public ItemStack modifyArgIndex3(ItemStack value) {
        return TextureDynamic.Hooks.toRenderItemStack(value);
    }
}
