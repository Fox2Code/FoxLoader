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

import com.fox2code.foxloader.config.NoConfigObject;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModInfo;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.updater.UpdateManager;
import com.fox2code.foxloader.utils.io.URLUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.common.util.Utils;
import net.minecraft.common.util.i18n.StringTranslate;

import java.util.Arrays;

public class GuiModMenu extends GuiScreen {
    private static final int BUTTON_MARGIN = 4;
    private GuiModMenuContainer modListContainer;
    GuiModMenuDescription modListDescription;
    private GuiSmallButton guiUpdateAll;
    private final GuiButton[] modActionButtons = new GuiButton[4];
    private final ModActionButtonType[] modActionButtonsTypes = new ModActionButtonType[4];
    private int selectedBackup = 0;
    private String screenTitle;

    public GuiModMenu(GuiScreen parent) {
        this.parentScreen = parent;
        this.screenTitle = "Mod Menu";
    }

    @Override
    public void initGui() {
        super.initGui();
        this.modListContainer = new GuiModMenuContainer(this);
        this.controlList.add(this.modListContainer);
        this.modListDescription = new GuiModMenuDescription(this);
        this.controlList.add(this.modListDescription);
        StringTranslate st = StringTranslate.getInstance();
        this.screenTitle = st.translateKeyFormat(
                "mods.title", ModLoaderInit.getModContainers().size());
        int widthOneSixth = Math.min(this.width / 6, 50 + (BUTTON_MARGIN * 6));
        int midWidth = this.width / 2;
        int buttonWidth = Math.max(Math.min(150, (widthOneSixth - BUTTON_MARGIN) * 2), 50);
        this.controlList.add(new GuiSmallButton(0,
                midWidth - (widthOneSixth * 3) + BUTTON_MARGIN,
                this.height - 24, buttonWidth, 20,
                st.translateKey("mods.openFolder")));
        this.controlList.add(new GuiSmallButton(1,
                midWidth - widthOneSixth + BUTTON_MARGIN,
                this.height - 24, buttonWidth, 20,
                st.translateKey("gui.done")));
        this.controlList.add(this.guiUpdateAll = new GuiSmallButton(2,
                midWidth + widthOneSixth + BUTTON_MARGIN,
                this.height - 24, buttonWidth, 20,
                st.translateKey("mods.updateAllMods")));
        // Mod Actions buttons
        for (int i = 0; i < this.modActionButtons.length; i++) {
            GuiSmallButton guiSmallButton = new GuiSmallButton(4 + i, 240, 32, "#" + i);
            this.modActionButtons[i] = guiSmallButton;
            this.controlList.add(guiSmallButton);
            guiSmallButton.visible = false;
        }
        Arrays.fill(this.modActionButtonsTypes, null);
        // Update GUI state after init
        this.modListContainer.elementClicked(this.selectedBackup, false);
    }

    @Override
    public void drawScreen(float var1, float var2, float deltaTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.screenTitle, (int) (this.width / 2), 13, 0xffffff);
        super.drawScreen(var1, var2, deltaTicks);
    }

    @Override
    protected void actionPerformed(GuiButton var1) {
        if (var1.id == 0) {
            Utils.openURL("file://" + ModLoader.getModsFolder().getPath());
        } else if (var1.id == 1) {
            this.mc.displayGuiScreen(this.parentScreen);
        } else if (var1.id == 2) {
            UpdateManager.getInstance().doUpdates();
        } else if (var1.id >= 4 && var1.id < 8) {
            ModActionButtonType type = this.modActionButtonsTypes[var1.id - 4];
            if (type != null) {
                type.doAction(this.modListContainer.getSelectedModContainer(), this);
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
        final ModContainer modContainer = this.modListContainer.getSelectedModContainer();
        this.guiUpdateAll.enabled = UpdateManager.getInstance().canUpdate();
        // Update mod action buttons
        this.updateActionButtons();
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

    void updateActionButtons() {
        final ModContainer modContainer = this.modListContainer.getSelectedModContainer();
        this.resetModButtonAction(0);
        Object configObject = modContainer.getConfigObject();
        ModInfo modInfo = modContainer.getModInfo();
        if (UpdateManager.getInstance().canUpdate(modContainer.getModId())) {
            this.addModButtonAction(ModActionButtonType.UPDATE);
        }
        if (configObject instanceof GuiConfigProviderConfigObject ||
                (configObject != null && !(configObject instanceof NoConfigObject))) {
            this.addModButtonAction(ModActionButtonType.CONFIGURE);
        }
        if (URLUtils.isValidHttpURL(modInfo.website)) {
            this.addModButtonAction(ModActionButtonType.WEBSITE);
        }
        int buttonCount = this.countButtonActions();
        if (buttonCount == 0) {
            this.addModButtonAction(ModActionButtonType.CONFIGURE);
            this.modActionButtons[0].enabled = false;
            buttonCount = 1;
        }
        int leftMost = 240 - BUTTON_MARGIN;
        int rightMost = this.width + BUTTON_MARGIN - 10;
        int mostPossibleButtons = (rightMost - leftMost) / (50 + (BUTTON_MARGIN * 2));
        if (mostPossibleButtons <= 0) {
            this.resetModButtonAction(0);
            return;
        } else if (mostPossibleButtons < buttonCount) {
            this.resetModButtonAction(mostPossibleButtons);
            buttonCount = mostPossibleButtons;
        }
        int totalWidth = rightMost - leftMost;
        int buttonSpace = totalWidth / buttonCount;
        int leakingWidth = totalWidth % buttonCount;
        int buttonWidth = buttonSpace - (BUTTON_MARGIN * 2);
        for (int i = 0; i < buttonCount; i++) {
            GuiButton guiButton = this.modActionButtons[i];
            guiButton.width = buttonWidth;
            guiButton.xPosition = leftMost + BUTTON_MARGIN + (buttonSpace * i);
            if (i != 0) {
                guiButton.xPosition += ((leakingWidth * i) / (buttonCount - 1));
            }
            this.modActionButtonsTypes[i].updateTitle(guiButton, this.fontRenderer, buttonWidth - 4);
        }
    }

    private void resetModButtonAction(int fromId) {
        for (int i = fromId; i < this.modActionButtons.length; i++) {
            this.modActionButtonsTypes[i] = null;
            GuiButton guiButton = this.modActionButtons[i];
            if (guiButton != null) {
                guiButton.visible = false;
                guiButton.enabled = true;
            }
        }
    }

    private void addModButtonAction(ModActionButtonType modActionButtonType) {
        for (int i = 0; i < this.modActionButtons.length; i++) {
            if (this.modActionButtonsTypes[i] == null) {
                this.modActionButtonsTypes[i] = modActionButtonType;
                GuiButton guiButton = this.modActionButtons[i];
                guiButton.displayString = StringTranslate.getInstance()
                        .translateKey(modActionButtonType.titleTranslate);
                guiButton.visible = true;
                break;
            }
        }
    }

    private int countButtonActions() {
        for (int i = 0; i < this.modActionButtons.length; i++) {
            if (this.modActionButtonsTypes[i] == null) {
                return i;
            }
        }
        return this.modActionButtons.length;
    }

    private enum ModActionButtonType {
        UPDATE("mods.update") {
            @Override
            void doAction(ModContainer modContainer, GuiModMenu guiModMenu) {
                UpdateManager.getInstance().doUpdate(modContainer.getModId());
                guiModMenu.updateActionButtons();
            }
        }, CONFIGURE("mods.configure") {
            @Override
            void doAction(ModContainer modContainer, GuiModMenu guiModMenu) {
                Object configObject = modContainer.getConfigObject();
                if (configObject instanceof GuiConfigProviderConfigObject) {
                    Minecraft.getInstance().displayGuiScreen(
                            ((GuiConfigProviderConfigObject) configObject)
                                    .provideConfigScreen(guiModMenu));
                } else if (!(configObject instanceof NoConfigObject)) {
                    guiModMenu.openModConfigScreen(modContainer);
                }
            }

            @Override
            public void updateTitle(GuiButton guiButton, FontRenderer fontRenderer, int buttonWidth) {
                StringTranslate st = StringTranslate.getInstance();
                String buttonText = st.translateKey("mods.configureMod");
                if (fontRenderer.getStringWidth(buttonText) >= buttonWidth) {
                    buttonText = st.translateKey(this.titleTranslate);
                }
                guiButton.displayString = buttonText;
            }
        }, WEBSITE("mods.website") {
            @Override
            void doAction(ModContainer modContainer, GuiModMenu guiModMenu) {
                String website = modContainer.getModInfo().website;
                if (URLUtils.isValidHttpURL(website)) {
                    Utils.openURL(website);
                }
            }
        };

        final String titleTranslate;

        ModActionButtonType(String titleTranslate) {
            this.titleTranslate = titleTranslate;
        }

        abstract void doAction(ModContainer modContainer, GuiModMenu guiModMenu);

        public void updateTitle(GuiButton guiButton, FontRenderer fontRenderer, int buttonWidth) {
            guiButton.displayString = StringTranslate.getInstance().translateKey(this.titleTranslate);
        }
    }
}
