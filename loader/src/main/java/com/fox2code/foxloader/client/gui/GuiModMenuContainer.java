/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
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

import com.fox2code.foxloader.loader.LoadingPlugin;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.updater.UpdateManager;
import com.indigo3d.util.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlotSimple;
import net.minecraft.client.renderer.world.Tessellator;
import net.minecraft.common.util.ChatColors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class GuiModMenuContainer extends GuiSlotSimple {
    private static final String DEFAULT_MOD_ICON = "assets/foxloader/textures/items/item_missing.png";
    private static final int ENTRY_WIDTH = 220;
    private static final int SIDE_OFFSET = 4;
    private static final int CONTENT_MARGIN = 2;
    private final GuiModMenu guiModList;
    private final ModContainer[] mods;
    private int selected = 0;

    public GuiModMenuContainer(GuiModMenu guiModList) {
        super(guiModList.width, guiModList.height,
                32, guiModList.height - 26, 36, ENTRY_WIDTH);
        this.left = SIDE_OFFSET;
        this.right = ENTRY_WIDTH + SIDE_OFFSET + (CONTENT_MARGIN * 2);
        this.scrollbarOffset = CONTENT_MARGIN;
        this.guiModList = guiModList;
        ArrayList<ModContainer> modContainers = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            modContainers.addAll(ModLoaderInit.getModContainers());
        }
        this.mods = modContainers.toArray(new ModContainer[0]);
    }

    @Override
    protected int getSize() {
        return this.mods.length;
    }

    @Override
    protected void elementClicked(int i, boolean b) {
        this.selected = i;
        this.guiModList.updateGuiState();
    }

    @Override
    protected boolean isSelected(int i) {
        return this.selected == i;
    }

    @Override
    public int getSelectedIndex() {
        return this.selected;
    }

    @Override
    protected void setSelectedIndex(int selected) {
        this.selected = selected;
        this.guiModList.updateGuiState();
    }

    @Override
    protected int getContentHeight() {
        return this.slotHeight * this.mods.length;
    }

    @NotNull public ModContainer getSelectedModContainer() {
        return this.mods[this.selected];
    }

    @Override
    protected void renderDecorations(float width, float height) {
        // RenderSystem.
    }

    @Override
    protected void drawSlot(Minecraft mc, int index, float x, float y, int iconHeight, Tessellator tessellator) {
        ModContainer modContainer = this.mods[index];
        ModInfo modInfo = modContainer.getModInfo();

        String iconPath = modInfo.iconPath;
        if (iconPath == null || iconPath.isEmpty()) {
            iconPath = DEFAULT_MOD_ICON;
        }
        RenderSystem.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.bindTexture2D("/" + iconPath);
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_I(16777215);
        tessellator.addVertexWithUV(x, y + (float)iconHeight, 0.0, 0.0, 1.0);
        tessellator.addVertexWithUV(x + 32.0F, y + (float)iconHeight, 0.0, 1.0, 1.0);
        tessellator.addVertexWithUV(x + 32.0F, y, 0.0, 1.0, 0.0);
        tessellator.addVertexWithUV(x, y, 0.0, 0.0, 0.0);
        tessellator.draw();

        int modDisplayFlags = modContainer.getModDisplayFlags();
        float iconPosX = x + this.slotWidth - 24;
        float iconPosY = y + 12;
        RenderSystem.bindTexture2D("/assets/foxloader/textures/gui/icons.png");
        float color = this.selected == index ? 1F : 0.5F;
        RenderSystem.color(color, color, color, 1F);
        RenderSystem.enableBlend();
        if ((modDisplayFlags & LoadingPlugin.DISPLAY_FLAG_DISABLED) != 0) {
            this.drawCustomIcon(iconPosX, iconPosY, 3);
            iconPosX -= 20;
        }
        if ((modDisplayFlags & LoadingPlugin.DISPLAY_FLAG_LIBRARY) != 0) {
            this.drawCustomIcon(iconPosX, iconPosY, 2);
            iconPosX -= 20;
        }
        if ((modDisplayFlags & LoadingPlugin.DISPLAY_FLAG_GAME_CONTENT) != 0) {
            this.drawCustomIcon(iconPosX, iconPosY, 1);
            iconPosX -= 20;
        }
        if ((modDisplayFlags & LoadingPlugin.DISPLAY_FLAG_MIXIN) != 0) {
            this.drawCustomIcon(iconPosX, iconPosY, 0);
        }
        RenderSystem.disableBlend();
        RenderSystem.color(1F, 1F, 1F, 1F);

        String name = UpdateManager.getInstance().getUpdateState(modInfo.id).colorPrefix +
                modInfo.name + " " + modInfo.version;
        if (modInfo.unofficial) {
            name += ChatColors.GRAY + " (Unofficial)";
        }
        name += ChatColors.RESET;
        this.guiModList.getFontRenderer().drawStringWithShadow(name, x + 34, y + 1, 0xffffff);
        this.guiModList.getFontRenderer().drawSplitStringWithShadow(modInfo.description, x + 34, y + 12, 184, 0xA0A0A0, 2);
    }

    private void drawCustomIcon(float x, float y, int icon) {
        this.drawTexturedModalRect(x, y, icon % 12 * 20, (icon / 12) * 20, 20, 20);
    }

    public void drawElement(Minecraft minecraft, float mouseXIn, float mouseYin, float deltaTicks) {
        if (minecraft.theWorld != null) {
            drawRect(this.left, this.top, this.right, this.bottom, 0x70101010);
        }
        super.drawElement(minecraft, mouseXIn, mouseYin, deltaTicks);
    }

    @Override
    public boolean allowTransparency() {
        return true;
    }

    @Override
    public boolean renderOverlayBackground() {
        return false;
    }

    @Override
    public boolean renderOverlayShadows() {
        return false;
    }

    @Override
    public boolean allowScrolling(float mouseXIn, float mouseYIn) {
        return !this.guiModList.modListDescription.mouseHoveredOver(null, mouseXIn, mouseYIn);
    }
}
