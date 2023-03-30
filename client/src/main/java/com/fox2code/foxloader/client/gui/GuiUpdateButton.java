package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.network.ChatColors;
import com.fox2code.foxloader.updater.UpdateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.GuiSmallButton;

public final class GuiUpdateButton extends GuiSmallButton {
    private final String text, textColor;

    public GuiUpdateButton(int var1, int var2, int var3, String var4) {
        super(var1, var2, var3, var4);
        this.text = var4;
        this.textColor = ChatColors.RAINBOW + var4 + ChatColors.RESET;
    }

    public GuiUpdateButton(int var1, int var2, int var3, int var4, int var5, String var6) {
        super(var1, var2, var3, var4, var5, var6);
        this.text = var6;
        this.textColor = ChatColors.RAINBOW + var6 + ChatColors.RESET;
    }

    @Override
    public void drawButton(Minecraft _mc, int var2, int var3) {
        this.displayString = UpdateManager.getInstance().hasUpdates() ? this.textColor : this.text;
        super.drawButton(_mc, var2, var3);
    }
}
