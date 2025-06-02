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
package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.player.PlayerEvent;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

/**
 * This event is sent when the player start breaking a block,
 * the event doesn't edit the world directly, but might edit the world indirectly.
 */
public final class PlayerStartBreakBlockEvent extends PlayerEvent implements Event.Cancellable {
    private final int x, y, z, facing;
    private final ItemStack heldItem;

    public PlayerStartBreakBlockEvent(EntityPlayer entityPlayer, int x, int y, int z, int facing, ItemStack heldItem) {
        super(entityPlayer);
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.heldItem = heldItem;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getFacing() {
        return this.facing;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }
}
