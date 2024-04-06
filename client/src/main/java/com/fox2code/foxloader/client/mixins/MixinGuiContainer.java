package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.network.ChatColors;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.src.client.gui.GuiContainer;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Inject(method = "getItemInfo", at = @At("RETURN"))
    public void getItemInfo(ItemStack itemstack, CallbackInfoReturnable<List<String>> cir) {
        List<String> strings = cir.getReturnValue();
        if (itemstack == null || strings == null || strings.isEmpty() ||
                !strings.get(strings.size() - 1).startsWith(ChatColors.DARK_GRAY + "#")) {
            return;
        }
        RegisteredItemStack registeredItemStack = ClientMod.toRegisteredItemStack(itemstack);
        if (registeredItemStack.hasCustomWorldItemScale()) {
            strings.add(strings.size() - 1, ChatColors.DARK_GRAY +
                    "World item scale: " + registeredItemStack.getWorldItemScale());
        }
    }
}
