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
package com.fox2code.foxloader.event.inventory;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.player.PlayerEvent;
import net.minecraft.common.block.container.Container;
import net.minecraft.common.block.container.Slot;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerClickSlotEvent extends PlayerEvent implements Event.Cancellable {
    private final Container container;
    private final int slotId, mouseButton, transferType;
    private final Slot slot;

    public PlayerClickSlotEvent(@NotNull Container container, int slotId, int mouseButton,
                                int transferType, @NotNull EntityPlayer entityPlayer) {
        super(entityPlayer);
        this.container = container;
        this.slotId = slotId;
        this.mouseButton = mouseButton;
        this.transferType = transferType;
        this.slot = slotId == -999 ? null : container.getSlot(slotId);
    }

    public @NotNull Container getContainer() {
        return this.container;
    }

    public int getSlotId() {
        return this.slotId;
    }

    public int getMouseButton() {
        return this.mouseButton;
    }

    public int getTransferType() {
        return this.transferType;
    }

    public @Nullable Slot getSlot() {
        return this.slot;
    }

    public @Nullable ItemStack getSlotItem() {
        return this.slotId != -999 ? (this.slot != null ? this.slot.getStack() : null) :
                this.getEntityPlayer().getPlayerDataInventory().getCursorStack();
    }
}
