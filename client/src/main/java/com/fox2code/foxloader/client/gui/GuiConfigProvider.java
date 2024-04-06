package com.fox2code.foxloader.client.gui;

import net.minecraft.src.client.gui.GuiScreen;

public interface GuiConfigProvider {
    GuiScreen provideConfigScreen(GuiScreen parent);
}
