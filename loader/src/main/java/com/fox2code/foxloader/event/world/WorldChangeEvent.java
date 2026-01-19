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
package com.fox2code.foxloader.event.world;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * You can listen to this event for world changes, {@link WorldChangeEvent} children
 * are not guaranteed to be {@link Cancellable}, but they will always tell you what in which world has been changed.
 */
public abstract class WorldChangeEvent extends WorldEvent {
    public WorldChangeEvent(World world) {
        super(world);
    }

    /**
     * @return source entity of this event, or {@code null} if no entity was linked to the event
     */
    @Nullable public abstract Entity getEntitySource();

    public abstract boolean doesChangeBlock(int x, int y, int z);

    public abstract boolean doesChangeBlockInArea(int x1, int y1, int z1, int x2, int y2, int z2);

    public abstract int getBlockChangeMinX();

    public abstract int getBlockChangeMaxX();

    public abstract int getBlockChangeMinY();

    public abstract int getBlockChangeMaxY();

    public abstract int getBlockChangeMinZ();

    public abstract int getBlockChangeMaxZ();

    /**
     * @return if the source of the change is the block being changed itself,
     * useful if you want to make a land protection plugin without disabling
     * core game mechanics.
     */
    public boolean isSelfOriginated() {
        return false;
    }

    /**
     * @return if the change only change metadata, and not the blocks itself,
     * like editing text, useful if you want to make a land protection plugin
     * with finer land claim control.
     */
    public boolean isMetadataOnly() {
        return false;
    }

    @Event.DelegateEvent
    public static abstract class SingleBlockChange extends WorldChangeEvent {
        private final int x, y, z;

        public SingleBlockChange(World world, int x, int y, int z) {
            super(world);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public final boolean doesChangeBlock(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }

        @Override
        public final boolean doesChangeBlockInArea(int x1, int y1, int z1, int x2, int y2, int z2) {
            return Math.min(x1, x2) <= this.x && Math.max(x1, x2) >= this.x &&
                    Math.min(y1, y2) <= this.y && Math.max(y1, y2) >= this.y &&
                    Math.min(z1, z2) <= this.z && Math.max(z1, z2) >= this.z;
        }

        @Override
        public int getBlockChangeMinX() {
            return this.x;
        }

        @Override
        public int getBlockChangeMaxX() {
            return this.x;
        }

        @Override
        public int getBlockChangeMinY() {
            return this.y;
        }

        @Override
        public int getBlockChangeMaxY() {
            return this.y;
        }

        @Override
        public int getBlockChangeMinZ() {
            return this.z;
        }

        @Override
        public int getBlockChangeMaxZ() {
            return this.z;
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
    }
}
