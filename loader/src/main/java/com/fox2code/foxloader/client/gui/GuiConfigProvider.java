package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.config.NoConfigObject;
import net.minecraft.client.gui.GuiScreen;

@FunctionalInterface
public interface GuiConfigProvider extends NoConfigObject {
    GuiScreen provideConfigScreen(GuiScreen parent);
}
