package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.loader.ModLoaderInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.world.Tessellator;
import net.minecraft.common.util.ChatColors;
import org.jetbrains.annotations.NotNull;

public class GuiModListContainer extends GuiSlot {
    private final GuiModList guiModList;
    private final ModContainer[] mods;
    private int selected = 0;

    public GuiModListContainer(GuiModList guiModList) {
        super(guiModList.width, guiModList.height,
                32, guiModList.height - 51, 36);
        this.guiModList = guiModList;
        this.mods = ModLoaderInit.getModContainers().toArray(new ModContainer[0]);
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

    @NotNull public ModContainer getSelectedModContainer() {
        return this.mods[this.selected];
    }

    @Override
    protected void drawSlot(Minecraft mc, int index, float x, float y, int iconHeight, Tessellator tessellator) {
        ModContainer modContainer = this.mods[index];
        ModInfo modInfo = modContainer.getModInfo();

        String name = // UpdateManager.getInstance().getUpdateState(modInfo.id).colorPrefix +
                modInfo.name + " " + modInfo.version;
        if (modInfo.unofficial) {
            name += ChatColors.GRAY + " (Unofficial)";
        }
        name += ChatColors.RESET;
        String id = modInfo.description;
        String file = modInfo.fileName + " (id: " + modInfo.id + ")";

        this.guiModList.drawString(this.guiModList.getFontRenderer(), name, x + 2, y + 1, 0xffffff);
        this.guiModList.drawString(this.guiModList.getFontRenderer(), id, x + 2, y + 12, 0x808080);
        this.guiModList.drawString(this.guiModList.getFontRenderer(), file, x + 2, y + 12 + 10, 0x808080);
    }
}
