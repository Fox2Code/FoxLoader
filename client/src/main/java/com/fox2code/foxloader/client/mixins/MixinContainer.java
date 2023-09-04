package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.gui.ContainerWrapped;
import net.minecraft.src.client.gui.Container;
import net.minecraft.src.client.gui.GuiContainer;
import net.minecraft.src.client.gui.Slot;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;


@Mixin(Container.class)
public class MixinContainer {
    @Inject(method = "onCraftGuiClosed", at = @At("HEAD"), cancellable = true)
    public void hotfix_onCraftGuiClosedHook(EntityPlayer var1, CallbackInfo ci) {
        if (var1 == null || var1.inventory == null) {
            ci.cancel();
        }
    }

    /**
     * Disable default behaviour on wrapped container for better stability.
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    public void onQuickMove(int var1, CallbackInfoReturnable<ItemStack> cir) {
        if (this instanceof ContainerWrapped) {
            cir.setReturnValue(null);
        }
    }
}
