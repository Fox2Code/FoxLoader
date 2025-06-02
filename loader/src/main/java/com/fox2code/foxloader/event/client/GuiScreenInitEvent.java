package com.fox2code.foxloader.event.client;

import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public final class GuiScreenInitEvent extends GuiScreenEvent {
    private final List<GuiElement> controlList;

    public GuiScreenInitEvent(GuiScreen guiScreen, List<GuiElement> controlList) {
        super(guiScreen);
        this.controlList = controlList;
    }

    public List<GuiElement> getControlList() {
        return this.controlList;
    }
}
