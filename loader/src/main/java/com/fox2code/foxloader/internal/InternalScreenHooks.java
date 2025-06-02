package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.client.GuiScreenInitEvent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public final class InternalScreenHooks {
    private static final EventHolder<GuiScreenInitEvent> GUI_SCREEN_INIT_EVENT =
            EventHolder.getHolderFromEvent(GuiScreenInitEvent.class);

    private InternalScreenHooks() {}

    public static void onGuiScreenInitHook(GuiScreen guiScreen, List<GuiElement> controlList) {
        if (GUI_SCREEN_INIT_EVENT.isEmpty()) return;
        new GuiScreenInitEvent(guiScreen, controlList).callEvent();
    }
}
