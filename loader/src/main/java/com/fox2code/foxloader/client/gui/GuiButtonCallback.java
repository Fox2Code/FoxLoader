package com.fox2code.foxloader.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonCallback extends GuiButton {
    private final Runnable callback;

    public GuiButtonCallback(int id, int x, int y, String text, Runnable callback) {
        super(id, x, y, text);
        this.callback = callback;
    }

    public GuiButtonCallback(int id, int x, int y, String text, String extraText, Runnable callback) {
        super(id, x, y, text, extraText);
        this.callback = callback;
    }

    public GuiButtonCallback(int id, int x, int y, int w, int h, String text, Runnable callback) {
        super(id, x, y, w, h, text);
        this.callback = callback;
    }

    public GuiButtonCallback(int id, int x, int y, int w, int h, String text, String extraText, Runnable callback) {
        super(id, x, y, w, h, text, extraText);
        this.callback = callback;
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, float x, float y) {
        boolean pressed = super.mousePressed(minecraft, x, y);
        if (pressed && this.callback != null) this.callback.run();
        return pressed;
    }
}
