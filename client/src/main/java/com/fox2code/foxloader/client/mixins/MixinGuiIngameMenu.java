package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.gui.GuiModList;
import com.fox2code.foxloader.client.gui.GuiUpdateButton;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
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
    public void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 1) {
            NetworkPlayer networkPlayer = (NetworkPlayer) Minecraft.getInstance().thePlayer;
            if (networkPlayer != null && networkPlayer.getConnectionType() ==
                    NetworkPlayer.ConnectionType.SINGLE_PLAYER) {
                ModLoader.Internal.notifyNetworkPlayerDisconnected(networkPlayer, null);
            }
        }

        if (button.id == 500) {
            this.mc.displayGuiScreen(new GuiModList(this));
            ci.cancel();
        }
    }
}
