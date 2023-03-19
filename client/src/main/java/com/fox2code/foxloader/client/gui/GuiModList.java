package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.client.gui.*;
import org.lwjgl.Sys;

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
        StringTranslate st = StringTranslate.getInstance();
        this.controlList.add(new GuiSmallButton(0,
                this.width / 2 - 154, this.height - 48,
                st.translateKey("mods.openFolder")));
        this.controlList.add(new GuiSmallButton(1,
                this.width / 2 + 4, this.height - 48,
                st.translateKey("gui.done")));
    }

    @Override
    public void drawScreen(int var1, int var2, float deltaTicks) {
        this.modListContainer.drawScreen(var1, var2, deltaTicks);
        super.drawScreen(var1, var2, deltaTicks);
    }

    @Override
    protected void actionPerformed(GuiButton var1) {
        if (var1.id == 0) {
            Sys.openURL("file://" + ModLoader.mods);
        } else if (var1.id == 1) {
            this.mc.displayGuiScreen(this.parent);
        } else {
            this.modListContainer.actionPerformed(var1);
        }
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }
}
