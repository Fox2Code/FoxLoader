package com.fox2code.foxloader.client.gui;

import net.minecraft.client.gui.GuiScreen;

@FunctionalInterface
public interface GuiConfigProviderConfigObject {
    GuiScreen provideConfigScreen(GuiScreen parent);
}
