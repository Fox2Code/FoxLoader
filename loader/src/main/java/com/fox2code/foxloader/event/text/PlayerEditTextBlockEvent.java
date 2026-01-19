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
package com.fox2code.foxloader.event.text;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.interaction.PlayerUseItemOnBlockEvent;
import com.fox2code.foxloader.event.world.WorldChangeEvent;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is used to monitor when a player changes the text of a block in the world, can be used for censoring text.
 * <p>
 * For handling player mutes, it is recommended to use {@link PlayerUseItemOnBlockEvent}
 */
@Event.DelegateEvent
public abstract class PlayerEditTextBlockEvent extends WorldChangeEvent.SingleBlockChange implements Event.Cancellable {
    private final EntityPlayer entityPlayer;

    public PlayerEditTextBlockEvent(World world, int x, int y, int z, EntityPlayer entityPlayer) {
        super(world, x, y, z);
        this.entityPlayer = entityPlayer;
    }

    @Override
    public @Nullable Entity getEntitySource() {
        return this.entityPlayer;
    }

    @Override
    public boolean isMetadataOnly() {
        return true;
    }

    public EntityPlayer getEntityPlayer() {
        return this.entityPlayer;
    }

    @NotNull public abstract String getOldBlockText();

    @NotNull public abstract String getNewBlockText();
}
