package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.ChatColors;
import com.fox2code.foxloader.updater.UpdateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.GuiSlot;
import net.minecraft.src.client.renderer.Tessellator;
import org.jetbrains.annotations.NotNull;

public class GuiModListContainer extends GuiSlot {
    private final GuiModList guiModList;
    private final ModContainer[] mods;
    private int selected = 0;

    public GuiModListContainer(GuiModList guiModList) {
        super(Minecraft.theMinecraft,
                guiModList.width,
                guiModList.height, 32,
                guiModList.height - 51, 36);
        this.guiModList = guiModList;
        this.mods = ModLoader.getModContainers().toArray(new ModContainer[0]);
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

    @NotNull
    public ModContainer getSelectedModContainer() {
        return this.mods[this.selected];
    }

    @Override
    protected void drawBackground() {
        this.guiModList.drawDefaultBackground();
    }

    @Override
    protected void drawSlot(int i, int i1, int i2, int i3, Tessellator tessellator) {
        ModContainer modContainer = this.mods[i];

        String name = UpdateManager.getInstance().getUpdateState(modContainer.id).colorPrefix +
                modContainer.name + " " + modContainer.version;
        if (modContainer.unofficial) {
            name += ChatColors.GRAY + " (Unofficial)";
        }
        name += ChatColors.RESET;
        String id = modContainer.description;
        String file = modContainer.getFileName() + " (id: " + modContainer.id + ")";

        this.guiModList.drawString(this.guiModList.getFontRenderer(), name, i1 + 2, i2 + 1, 0xffffff);
        this.guiModList.drawString(this.guiModList.getFontRenderer(), id, i1 + 2, i2 + 12, 0x808080);
        this.guiModList.drawString(this.guiModList.getFontRenderer(), file, i1 + 2, i2 + 12 + 10, 0x808080);
    }
}
