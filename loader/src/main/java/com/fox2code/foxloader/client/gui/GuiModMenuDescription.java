/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.world.Tessellator;

import java.util.ArrayList;

public class GuiModMenuDescription extends GuiSlot {
    private static final int SIDE_PANEL_WIDTH = 240;
    private static final int EXTRA_PADDING = 10;
    private static final int ACTION_BUTTONS_HEIGHT = 20;
    private final ArrayList<String> description = new ArrayList<>();
    private final FontRenderer fontRenderer;
    private int usedContentHeight = 0;

    GuiModMenuDescription(GuiModMenu guiModList) {
        super(guiModList.width, guiModList.height,
                32 + ACTION_BUTTONS_HEIGHT, guiModList.height - 26,
                guiModList.getFontRenderer().FONT_HEIGHT,
                guiModList.width - SIDE_PANEL_WIDTH - EXTRA_PADDING);
        this.left = SIDE_PANEL_WIDTH;
        this.right = guiModList.width - EXTRA_PADDING;
        this.fontRenderer = guiModList.getFontRenderer();
    }

    @Override
    protected int getSize() {
        return this.description.size();
    }

    @Override
    protected int getContentHeight() {
        return Math.max(this.usedContentHeight, this.bottom - this.top - 4);
    }

    @Override
    protected int getSlotHeight(int slotIndex) {
        String text = this.description.get(slotIndex);
        return Math.max(this.fontRenderer.FONT_HEIGHT,
                this.fontRenderer.getSplitStringHeight(text, this.slotWidth));
    }

    @Override
    protected void elementClicked(int i, boolean b) {}

    @Override
    protected boolean isSelected(int i) {
        return false;
    }

    @Override
    protected void drawBackground() {}

    @Override
    protected void drawSlot(Minecraft minecraft, int slotIndex, float x, float y, int smthYAxis, Tessellator tessellator) {
        this.fontRenderer.drawSplitStringWithShadow(
                this.description.get(slotIndex),
                x, y, this.slotWidth, 0xFFFFFF);
    }

    @Override
    public boolean renderOverlayShadows() {
        return false;
    }

    @Override
    public boolean renderOverlayBackground() {
        return false;
    }

    @Override
    public boolean allowTransparency() {
        return true;
    }

    @Override
    public boolean allowScrolling(float mouseXIn, float mouseYIn) {
        return this.mouseHoveredOver(null, mouseXIn, mouseYIn);
    }

    public void resetText() {
        this.description.clear();
        this.usedContentHeight = 0;
        this.scrollToInbound();
    }

    public void addText(String line) {
        this.description.add(line);
        this.usedContentHeight += this.getSlotHeight(
                this.description.size() - 1);
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, float x, float y) {
        return false;
    }
}
