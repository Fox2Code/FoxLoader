package com.fox2code.foxloader.client.gui;

import net.minecraft.src.client.gui.FontRenderer;
import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.StringTranslate;

public class GuiModList extends GuiScreen {
    private final GuiScreen parent;
    private GuiModListContainer modListContainer;

    public GuiModList(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.modListContainer = new GuiModListContainer(this);
        this.modListContainer.registerScrollButtons(this.controlList, 4, 5);
        this.controlList.add(new GuiButton(0,
                this.width / 2 + 4, this.height - 28, 150, 20,
                StringTranslate.getInstance().translateKey("gui.cancel")));
    }

    @Override
    public void drawScreen(int var1, int var2, float deltaTicks) {
        this.modListContainer.drawScreen(var1, var2, deltaTicks);
        super.drawScreen(var1, var2, deltaTicks);
    }

    @Override
    protected void actionPerformed(GuiButton var1) {
        if (var1.id == 0) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        this.modListContainer.actionPerformed(var1);
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }
}
