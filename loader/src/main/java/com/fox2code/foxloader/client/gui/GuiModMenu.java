package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.config.NoConfigObject;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.updater.UpdateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSmallButton;
import net.minecraft.common.util.i18n.StringTranslate;
import org.lwjgl.Sys;

public class GuiModMenu extends GuiScreen {
    private final GuiScreen parent;
    private GuiModMenuContainer modListContainer;
    GuiModMenuDescription modListDescription;
    private GuiSmallButton guiUpdateAll, guiConfigureMod;
    private Object guiScreen;
    private int selectedBackup = 0;

    public GuiModMenu(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.modListContainer = new GuiModMenuContainer(this);
        this.controlList.add(this.modListContainer);
        this.modListDescription = new GuiModMenuDescription(this);
        this.controlList.add(this.modListDescription);
        StringTranslate st = StringTranslate.getInstance();
        this.controlList.add(new GuiSmallButton(0,
                this.width / 2 - 154, this.height - 48,
                st.translateKey("mods.openFolder")));
        this.controlList.add(new GuiSmallButton(1,
                this.width / 2 + 4, this.height - 48,
                st.translateKey("gui.done")));
        this.controlList.add(this.guiUpdateAll = new GuiSmallButton(2,
                this.width / 2 - 154, this.height - 24,
                st.translateKey("mods.updateAllMods")));
        this.controlList.add(this.guiConfigureMod = new GuiSmallButton(3,
                this.width / 2 + 4, this.height - 24,
                st.translateKey("mods.configureMod")));
        // Update GUI state after init
        this.modListContainer.elementClicked(this.selectedBackup, false);
    }

    @Override
    public void drawScreen(float var1, float var2, float deltaTicks) {
        this.drawDefaultBackground();
        super.drawScreen(var1, var2, deltaTicks);
    }

    @Override
    protected void actionPerformed(GuiButton var1) {
        if (var1.id == 0) {
            Sys.openURL("file://" + ModLoader.getModsFolder().getPath());
        } else if (var1.id == 1) {
            this.mc.displayGuiScreen(this.parent);
        } else if (var1.id == 2) {
            UpdateManager.getInstance().doUpdates();
        } else if (var1.id == 3) {
            if (this.guiScreen instanceof GuiConfigProvider) {
                Minecraft.getInstance().displayGuiScreen(
                        ((GuiConfigProvider) this.guiScreen).provideConfigScreen(this));
            } else {
                this.openModConfigScreen(this.modListContainer.getSelectedModContainer());
            }
        } else {
            this.modListContainer.actionPerformed(var1);
        }
    }

    // If somehow you have a better implementation, go ahead
    private void openModConfigScreen(ModContainer modContainer) {
        Minecraft.getInstance().displayGuiScreen(new GuiModConfig(this, modContainer));
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }

    void updateGuiState() {
        this.selectedBackup = this.modListContainer.getSelectedIndex();
        final StringTranslate st = StringTranslate.getInstance();
        final ModContainer modContainer = this.modListContainer.getSelectedModContainer();
        this.guiUpdateAll.enabled = UpdateManager.getInstance().canUpdate();
        this.guiScreen = null;
        this.guiConfigureMod.displayString = st.translateKey("mods.configureMod");
        Object configObject = modContainer.getConfigObject();
        if (configObject instanceof GuiConfigProvider) {
            this.guiScreen = modContainer.getConfigObject();
            this.guiConfigureMod.enabled = true;
        } else if (configObject == null) {
            this.guiConfigureMod.enabled = false;
        } else {
            this.guiConfigureMod.enabled = !(configObject instanceof NoConfigObject);
        }
        // Update mod description
        ModInfo modInfo = modContainer.getModInfo();
        String fileName = modInfo.fileName;
        if (FoxLauncher.DEVELOPING_FOXLOADER && "foxloader".equals(modInfo.id) && "main".equals(fileName)) {
            fileName = "FoxLoader-" + BuildConfig.FOXLOADER_VERSION + ".jar"; // Pretend jar name in development mode.
        }
        this.modListDescription.resetText();
        this.modListDescription.addText("File: " + fileName);
        this.modListDescription.addText("Name: " + modInfo.name);
        this.modListDescription.addText("Id: " + modInfo.id);
        this.modListDescription.addText("Version: " + modInfo.version);
        this.modListDescription.addText("Authors: " + modInfo.authors);
        this.modListDescription.addText("Description: " + modInfo.description);
    }
}
