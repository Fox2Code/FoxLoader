package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.client.gui.GuiContainer;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Redirect(method = "getItemInfo", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z"))
    public boolean redirectIsKeyDown(int key) {
        return ModLoader.DEV_MODE || Keyboard.isKeyDown(key);
    }
}
