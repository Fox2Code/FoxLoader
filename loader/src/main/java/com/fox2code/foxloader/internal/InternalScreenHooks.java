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
package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.client.GuiItemInfoEvent;
import com.fox2code.foxloader.event.client.GuiScreenInitEvent;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.common.item.ItemStack;

import java.util.List;

public final class InternalScreenHooks {
    private static final EventHolder<GuiScreenInitEvent> GUI_SCREEN_INIT_EVENT =
            EventHolder.getHolderFromEvent(GuiScreenInitEvent.class);
    private static final EventHolder<GuiItemInfoEvent> GUI_ITEM_INFO_EVENT =
            EventHolder.getHolderFromEvent(GuiItemInfoEvent.class);

    private InternalScreenHooks() {}

    public static void onGuiScreenInitHook(GuiScreen guiScreen, List<GuiElement> controlList) {
        if (GUI_SCREEN_INIT_EVENT.isEmpty()) return;
        GUI_SCREEN_INIT_EVENT.callEvent(new GuiScreenInitEvent(guiScreen, controlList));
    }

    public static List<String> onGuiGetItemInfoHook(List<String> description, GuiScreen guiScreen, ItemStack itemStack) {
        if (GUI_ITEM_INFO_EVENT.isEmpty()) return description;
        GuiItemInfoEvent guiItemInfoEvent = new GuiItemInfoEvent(guiScreen, itemStack, description);
        GUI_ITEM_INFO_EVENT.callEvent(guiItemInfoEvent);
        return guiItemInfoEvent.getDescription();
    }
}
