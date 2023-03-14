package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.launcher.BuildConfig;
import net.minecraft.src.client.gui.GuiMainMenu;
import net.minecraft.src.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {
    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/client/gui/GuiScreen;drawScreen(IIF)V"))
    public void onDrawGuiScreen(int n, int n2, float deltaTicks, CallbackInfo ci) {
        this.drawString(this.fontRenderer, "FoxLoader " + BuildConfig.FOXLOADER_VERSION, 2, this.height - 34, 16777215);
        this.drawCenteredString(this.fontRenderer,
                "You are using an Experimental Version of FoxLoader", this.width / 2, this.height / 4 + 36, 16764108);
    }
}
