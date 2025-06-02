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

import com.fox2code.foxloader.config.*;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.loader.ModLoaderOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.common.util.i18n.StringTranslate;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

final class GuiModConfig extends GuiScreen {
    private final ArrayList<GuiTextFieldModOption> modOptionsTextFields = new ArrayList<>();
    private String screenTitle = "Options";
    private final GuiScreen parentScreen;
    private final ModContainer modContainer;
    private final ConfigMenu configMenu;
    private final Object rootInstance;
    private final Object curInstance;

    public GuiModConfig(GuiScreen parentScreen, ModContainer modContainer) {
        this(parentScreen, modContainer, modContainer.getConfigObject());
    }

    public GuiModConfig(GuiScreen parentScreen, ModContainer modContainer, Object rootInstance) {
        this(parentScreen, modContainer, ConfigStructure.parseFromClass(
                rootInstance.getClass(), modContainer).rootConfigMenu, rootInstance, rootInstance);
    }

    private GuiModConfig(GuiScreen parentScreen, ModContainer modContainer, ConfigMenu configMenu,
                         Object rootInstance, Object curInstance) {
        this.parentScreen = parentScreen;
        this.modContainer = modContainer;
        this.configMenu = configMenu;
        this.rootInstance = rootInstance;
        this.curInstance = curInstance;
    }

    @Override
    public void initGui() {
        this.controlList.clear();
        this.modOptionsTextFields.clear();
        StringTranslate st= StringTranslate.getInstance();
        this.screenTitle = st.translateKey(this.configMenu.menuName);
        for(int i = 0; i < this.configMenu.configKeys.size(); ++i) {
            ConfigKey configKey = configMenu.configKeys.get(i);
            final int x = this.width / 2 - 155 + i % 2 * 160;
            final int y = this.height / 6 + 24 * (i >> 1);
            String translation = configKey.translation;
            String translated;
            ModLoaderInit.getModLoaderLogger().info("TR: " + translation);
            if (translation.equals(translated = st.translateKey(translation))) {
                String name = configKey.configEntry.configName();
                if (name != null && !name.isEmpty()) {
                    translated = name;
                }
            }
            switch (configKey.configElement) {
                case DUPLICATE:
                case BUTTON: {
                    GuiSmallButtonModConfig guiSmallButton = new GuiSmallButtonModConfig(
                            i,
                            this.width / 2 - 155 + i % 2 * 160,
                            this.height / 6 + 24 * (i >> 1),
                            translated
                    );
                    if (configKey.configElement == ConfigKey.ConfigElement.DUPLICATE ||
                            (configKey.configEntryType == ConfigEntryType.SUBMENU &&
                                    configKey.getField(this.curInstance) == null)) {
                        guiSmallButton.enabled = false;
                    }
                    if (configKey.configEntryType == ConfigEntryType.CONFIG) {
                        Object value = configKey.getField(this.curInstance);
                        if (value instanceof Boolean) {
                            value = st.translateKey(((Boolean) value) ? "gui.yes" : "gui.no");
                        }
                        guiSmallButton.displayString = translated + ": " + value;
                    }
                    this.controlList.add(guiSmallButton);
                    break;
                }
                case SLIDER: {
                    Object value = configKey.getField(this.curInstance);
                    this.controlList.add(new GuiSliderModConfig(i, x, y,
                            translated, value, this.curInstance, configKey));
                    break;
                }
                case TEXT: {
                    this.modOptionsTextFields.add(new GuiTextFieldModOption(x, y, this.curInstance, configKey));
                    break;
                }
            }
        }
        if (!this.modOptionsTextFields.isEmpty()) {
            Keyboard.enableRepeatEvents(true);
        }

        this.controlList.add(new GuiButton(200, this.width / 2 - 100, this.height / 6 + 168, st.translateKey("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id < 200 && button instanceof GuiSmallButtonModConfig) {
                GuiSmallButtonModConfig guiSmallButtonModConfig =
                        (GuiSmallButtonModConfig) button;
                ConfigKey configKey = this.configMenu.configKeys.get(button.id);
                Class<?> type = configKey.field.getType();
                switch (configKey.configEntryType) {
                    case CONFIG: {
                        boolean updated = false;
                        if (type == boolean.class) {
                            configKey.setField(this.curInstance, !(Boolean)
                                    configKey.getField(this.curInstance));
                            updated = true;
                        } else if (type.isEnum()) {
                            int ordinal = ((Enum<?>) configKey.getField(this.curInstance)).ordinal();
                            Enum<?>[] enums = (Enum<?>[]) type.getEnumConstants();
                            configKey.setField(this.curInstance,
                                    enums[(ordinal + 1) % enums.length]);
                            updated = true;
                        } else if (type == Void.class) {
                            updated = true;
                        }
                        if (updated) {
                            configKey.callHandler(this.curInstance);
                            Object newValue = configKey.getField(this.curInstance);
                            if (newValue instanceof Boolean) {
                                newValue = StringTranslate.getInstance().translateKey(
                                        ((Boolean) newValue) ? "gui.yes" : "gui.no");
                            }
                            guiSmallButtonModConfig.displayString =
                                    guiSmallButtonModConfig.text + ": " + newValue;
                        }
                        break;
                    }
                    case SUBMENU: {
                        configKey.callHandler(this.curInstance);
                        Object instance = configKey.getField(this.curInstance);
                        if (this.mc.currentScreen == this && instance != null) {
                            if (instance instanceof GuiConfigProvider) {
                                this.mc.displayGuiScreen(((GuiConfigProvider) instance)
                                        .provideConfigScreen(this));
                            } else if (instance instanceof GuiScreen) {
                                this.mc.displayGuiScreen((GuiScreen) instance);
                            } else {
                                this.mc.displayGuiScreen(new GuiModConfig(this, this.modContainer,
                                        configKey.configMenu, this.rootInstance, instance));
                            }
                        }
                        break;
                    }
                    case LINK: {
                        configKey.callHandler(this.curInstance);
                        Object link = configKey.getField(this.curInstance);
                        if (link instanceof Runnable) {
                            ((Runnable) link).run();
                        } else if (this.mc.currentScreen == this && link != null) {
                            this.mc.displayGuiScreen(new GuiLinkConfirm(this, link.toString()));
                        }
                        break;
                    }
                }
            }

            if (button.id == 200) {
                this.mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    @Override
    public void mouseClicked(float x, float y, int click) {
        super.mouseClicked(x, y, click);
        for (GuiTextFieldModOption guiTextFieldModOption :
                this.modOptionsTextFields) {
            guiTextFieldModOption.mouseClicked(x, y, click);
        }
    }

    @Override
    public void keyTyped(char c, int i) {
        for (GuiTextFieldModOption guiTextFieldModOption :
                this.modOptionsTextFields) {
            if (guiTextFieldModOption.isFocused) {
                guiTextFieldModOption.textboxKeyTyped(c, i);
            }
        }
    }

    @Override
    public void drawScreen(float mouseX, float mouseY, float deltaTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2F, 20, 16777215);
        for (GuiTextFieldModOption guiTextFieldModOption :
                this.modOptionsTextFields) {
            guiTextFieldModOption.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, deltaTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (this.rootInstance == this.modContainer.getConfigObject()) {
            ConfigIO.writeConfiguration(this.modContainer, this.rootInstance);
        }
    }

    private static class GuiSmallButtonModConfig extends GuiSmallButton {
        public final String text;

        public GuiSmallButtonModConfig(int id, int x, int y, String text) {
            super(id, x, y, text);
            this.text = text;
        }


    }

    private static class GuiSliderModConfig extends GuiSlider {
        private final String translateBase;
        private final Object curInstance;
        private final ConfigKey configKey;
        private final boolean clamp;
        private final double lowerBounds, upperBounds, boundsSize;

        public GuiSliderModConfig(int id, int x, int y, String translateBase, Object nativeValue,
                                  Object curInstance, ConfigKey configKey) {
            super(id, x, y, translateBase + ": " + nativeValue, 0f);
            this.translateBase = translateBase;
            this.curInstance = curInstance;
            this.configKey = configKey;
            Class<?> cls = configKey.field.getType();
            this.clamp = cls == byte.class || cls == short.class || cls == int.class || cls == long.class;
            if (this.clamp) {
                this.lowerBounds = Math.ceil(configKey.configEntry.lowerBounds());
                this.upperBounds = Math.floor(configKey.configEntry.upperBounds());
            } else {
                this.lowerBounds = configKey.configEntry.lowerBounds();
                this.upperBounds = configKey.configEntry.upperBounds();
            }
            this.boundsSize = this.upperBounds - this.lowerBounds;
            if (this.boundsSize <= 0) {
                this.enabled = false;
            } else {
                this.updateSliderValue();
            }
        }

         private double getModConfigValue() {
            Number number = null;
             try {
                 number = (Number) this.configKey.field.get(curInstance);
             } catch (Exception ignored) {}
             return number == null ? this.lowerBounds : number.doubleValue();
         }

         private void setModConfigValue(double value) {
             try {
                 Object nativeValue =  ConfigStructure.Internal
                         .correctNumber(value, this.configKey.field.getType());
                 this.displayString = this.translateBase + ": " + nativeValue;
                 this.configKey.field.set(this.curInstance, nativeValue);
                 this.configKey.callHandler(this.curInstance);
             } catch (Exception ignored) {}
         }

        @Override
        protected void mouseDragged(Minecraft var1, float var2, float var3) {
            super.mouseDragged(var1, var2, var3);
            if (this.visible && this.dragging) {
                this.applySliderChanges();
            }
        }

        @Override
        public boolean mousePressed(Minecraft var1, float var2, float var3) {
            boolean b = super.mousePressed(var1, var2, var3);
            if (this.visible && this.dragging) {
                this.applySliderChanges();
            }
            return b;
        }

        private void applySliderChanges() {
            if (this.enabled) {
                this.setModConfigValue((this.sliderValue * this.boundsSize) + this.lowerBounds);
                if (this.clamp) {
                    this.updateSliderValue();
                }
            }
        }

        private void updateSliderValue() {
            if (this.enabled) {
                this.sliderValue = (float) ((getModConfigValue() - this.lowerBounds) / this.boundsSize);
            }
        }
    }

    private static class GuiTextFieldModOption extends GuiTextField {
        private final Object curInstance;
        private final ConfigKey configKey;

        public GuiTextFieldModOption(int x, int y, Object curInstance, ConfigKey configKey) {
            super(x, y, 150, 20, "");
            this.curInstance = curInstance;
            this.configKey = configKey;
            String text = this.getModConfigValue();
            this.setText(text);
            this.setCursorPosition(text.length());
            int i = (int) configKey.configEntry.upperBounds();
            this.setMaxStringLength(i > 1 ? i : 255);
        }

        private String getModConfigValue() {
            String string = null;
            try {
                string = (String) this.configKey.field.get(curInstance);
            } catch (Exception ignored) {}
            return string == null ? "" : string;
        }

        private void setModConfigValue(String value) {
            try {
                this.configKey.field.set(this.curInstance, value);
                this.configKey.callHandler(this.curInstance);
            } catch (Exception ignored) {}
        }

        @Override
        public void textboxKeyTyped(char eventChar, int eventKey) {
            super.textboxKeyTyped(eventChar, eventKey);
            if (this.isEnabled && this.isFocused) {
                this.setModConfigValue(this.text);
            }
        }
    }
}
