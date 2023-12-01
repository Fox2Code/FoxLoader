package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.ResourceReloadingHelper;
import com.fox2code.foxloader.client.gui.GuiModList;
import com.fox2code.foxloader.client.gui.GuiUpdateButton;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.ChatColors;
import net.minecraft.src.client.Session;
import net.minecraft.src.client.gui.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {
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

    @Redirect(method = "drawScreen", at = @At(value = "FIELD", target =
            "Lnet/minecraft/src/client/Session;username:Ljava/lang/String;"))
    public String onGetUsername(Session instance) {
        final String username = instance.username;
        if (ModLoader.Contributors.hasContributorName(username)) {
            return ChatColors.RAINBOW + username;
        }
        return username;
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/client/gui/GuiScreen;drawScreen(IIF)V"))
    public void onDrawGuiScreen(int n, int n2, float deltaTicks, CallbackInfo ci) {
        this.drawString(this.fontRenderer, "FoxLoader " + BuildConfig.FOXLOADER_VERSION, 2, this.height - 20, 16777215);
        String extraMessage = null;
        if (ModLoader.I_AM_EXPERIMENTAL) {
            extraMessage = "You are using an Experimental Version of FoxLoader";
        }
        if (ResourceReloadingHelper.hasResourceError()) {
            extraMessage = "Some game resources failed to load properly!";
        }
        if (extraMessage != null) {
            this.drawCenteredString(this.fontRenderer, extraMessage,
                    this.width / 2, this.height / 4 + 36, 0xcc2200);
        }
    }
}
