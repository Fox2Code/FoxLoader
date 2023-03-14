package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.gui.GuiLinkConfirm;
import com.fox2code.foxloader.launcher.utils.NetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.ChatLine;
import net.minecraft.src.client.gui.GuiChat;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * @see net.minecraft.src.client.gui.GuiIngame
 */
@Mixin(GuiChat.class)
public class MixinGuiChat extends GuiScreen {
    @Shadow public int scrollChat;

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    public void onMouseClickedHook(int x, int y, int clickType, CallbackInfo ci) {
        if (clickType == 0) {
            int cursor = x - 2;
            if (cursor < 0 || cursor > 320) return;
            int rawIndex = y - ScaledResolution.instance.getScaledHeight() + 48;
            rawIndex = this.scrollChat - rawIndex / 9;
            if (rawIndex < 0) return;
            final List<ChatLine> chatMessageList =
                    Minecraft.theMinecraft.ingameGUI.chatMessageList;
            int maxIndex = Math.min(20 + this.scrollChat, chatMessageList.size());
            if (rawIndex >= maxIndex) return;
            ChatLine clickedLine = chatMessageList.get(rawIndex);
            String message = clickedLine.message;
            int start = message.indexOf("https://");
            if (start == -1) return;
            int end = start;
            while (end < message.length() &&
                    message.charAt(end) != ' ') {
                end++;
            }
            String preLink = message.substring(0, start);
            int startCursor = this.fontRenderer.getStringWidth(preLink);
            if (cursor < startCursor) return;
            String link = message.substring(start, end);
            if (!NetUtils.isValidURL(link)) return;
            int endCursor = startCursor + this.fontRenderer.getStringWidth(link);
            if (cursor > endCursor) return;
            Minecraft.getInstance().displayGuiScreen(new GuiLinkConfirm(this, link));
        }
    }
}
