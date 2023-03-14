package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientModLoader;
import net.minecraft.src.client.gui.GuiDebug;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiDebug.class)
public class MixinGuiDebug {

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z"))
    private boolean isKeyDown(int key) {
        switch (key) {
            default:
                // Fallback, but we should never reach here
                return Keyboard.isKeyDown(key);
            case Keyboard.KEY_Z:
                return ClientModLoader.showFrameTimes;
            case Keyboard.KEY_X:
                return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        }
    }
}
