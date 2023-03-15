package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.gui.GuiModList;
import com.fox2code.foxloader.launcher.BuildConfig;
import net.minecraft.src.client.gui.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {
    @Inject(method = "initGui", at = @At(value = "RETURN"))
    public void onInitGui(CallbackInfo ci) {
        this.controlList.add(new GuiSmallButton(500, this.width - 61, 25, 60, 20, "Mods"));
    }

    @Inject(method = "actionPerformed", at = @At(value = "HEAD"), cancellable = true)
    public void onActionPerformed(GuiButton var1, CallbackInfo ci) {
        if (var1.id == 500) {
            this.mc.displayGuiScreen(new GuiModList(this));
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/client/gui/GuiScreen;drawScreen(IIF)V"))
    public void onDrawGuiScreen(int n, int n2, float deltaTicks, CallbackInfo ci) {
        this.drawString(this.fontRenderer, "FoxLoader " + BuildConfig.FOXLOADER_VERSION, 2, this.height - 34, 16777215);
        this.drawCenteredString(this.fontRenderer,
                "You are using an Experimental Version of FoxLoader", this.width / 2, this.height / 4 + 36, 16764108);
    }
}
