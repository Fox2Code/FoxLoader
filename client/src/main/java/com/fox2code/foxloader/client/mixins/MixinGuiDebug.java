package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.NetClientHandlerExtensions;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.network.SidedMetadataAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.fox2code.ChatColors;
import net.minecraft.src.client.gui.FontRenderer;
import net.minecraft.src.client.gui.GuiDebug;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(GuiDebug.class)
public class MixinGuiDebug {
    @Shadow private Minecraft mc;

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/client/gui/GuiDebug;drawStringWithBg(Lnet/minecraft/src/client/gui/FontRenderer;Ljava/lang/String;III)V",
            ordinal = 0))
    private void drawVersionStringWith(GuiDebug instance, FontRenderer fr, String s, int x, int y, int fontcolor) {
        instance.drawStringWithBg(fr, s + " (FoxLoader " + BuildConfig.FOXLOADER_VERSION + ")", x, y, fontcolor);
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/client/gui/GuiDebug;drawStringWithBg(Lnet/minecraft/src/client/gui/FontRenderer;Ljava/lang/String;III)V",
            ordinal = 5))
    private void drawServerStringWith(GuiDebug instance, FontRenderer fr, String s, int x, int y, int fontcolor) {
        instance.drawStringWithBg(fr, s + ClientModLoader.Internal.getColoredServerNameDebugExt(), x, y, fontcolor);
    }

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
