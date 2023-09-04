package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.gui.GuiModList;
import com.fox2code.foxloader.client.gui.GuiUpdateButton;
import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiIngameMenu;
import net.minecraft.src.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public class MixinGuiIngameMenu extends GuiScreen {
    @Inject(method = "initGui", at = @At(value = "RETURN"))
    public void onInitGui(CallbackInfo ci) {
        this.controlList.add(new GuiUpdateButton(500, this.width - 62, 2, 60, 20, "Mods"));
    }

    @Inject(method = "actionPerformed", at = @At(value = "HEAD"), cancellable = true)
    public void onActionPerformed(GuiButton var1, CallbackInfo ci) {
        if (var1.id == 500) {
            this.mc.displayGuiScreen(new GuiModList(this));
            ci.cancel();
        }
    }
}
