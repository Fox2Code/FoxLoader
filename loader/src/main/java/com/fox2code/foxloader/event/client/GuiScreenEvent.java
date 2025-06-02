package com.fox2code.foxloader.event.client;

import com.fox2code.foxevents.Event;
import net.minecraft.client.gui.GuiScreen;

public abstract class GuiScreenEvent extends Event {
    private final GuiScreen guiScreen;

    public GuiScreenEvent(GuiScreen guiScreen) {
        this.guiScreen = guiScreen;
    }

    public GuiScreen getGuiScreen() {
        return this.guiScreen;
    }
}
