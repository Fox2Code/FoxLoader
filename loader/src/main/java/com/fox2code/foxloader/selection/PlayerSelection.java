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
package com.fox2code.foxloader.selection;

import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.world.World;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Represent a player selection, made with a wooden axe in creative.
 */
public abstract class PlayerSelection {
    private final WeakReference<EntityPlayer> entityPlayerWeakReference;

    public PlayerSelection(EntityPlayer entityPlayer) {
        Objects.requireNonNull(entityPlayer, "entityPlayer");
        this.entityPlayerWeakReference = new WeakReference<>(entityPlayer);
    }

    public final EntityPlayer getPlayer() {
        return this.entityPlayerWeakReference.get();
    }

    public abstract boolean hasSelection();

    public World getSelectionWorld() {
        if (!this.hasSelection()) return null;
        EntityPlayer entityPlayer = this.getPlayer();
        return entityPlayer == null ? null :
                entityPlayer.worldObj;
    }

    public abstract int getX1();

    public abstract int getX2();

    public abstract int getY1();

    public abstract int getY2();

    public abstract int getZ1();

    public abstract int getZ2();

    public int getMinX() {
        return Math.min(this.getX1(), this.getX2());
    }

    public int getMaxX() {
        return Math.max(this.getX1(), this.getX2());
    }

    public int getMinY() {
        return Math.min(this.getY1(), this.getY2());
    }

    public int getMaxY() {
        return Math.max(this.getY1(), this.getY2());
    }

    public int getMinZ() {
        return Math.min(this.getZ1(), this.getZ2());
    }

    public int getMaxZ() {
        return Math.max(this.getZ1(), this.getZ2());
    }

    public long getSelectionSize() {
        if (!this.hasSelection()) return 0;
        return (Math.abs(this.getX1() - this.getX2()) + 1L) *
                (Math.abs(this.getY1() - this.getY2()) + 1L) *
                (Math.abs(this.getZ1() - this.getZ2()) + 1L);
    }
}
