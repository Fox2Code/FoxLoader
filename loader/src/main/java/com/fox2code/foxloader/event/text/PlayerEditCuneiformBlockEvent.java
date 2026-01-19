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
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Event.DelegateEvent
public final class PlayerEditCuneiformBlockEvent extends PlayerEditTextBlockEvent {
    private final String oldBlockText;
    private String newBlockText;
    private final boolean finish;

    public PlayerEditCuneiformBlockEvent(World world, int x, int y, int z, EntityPlayer entityPlayer,
                                         String oldBlockText, String newBlockText, boolean finish) {
        super(world, x, y, z, entityPlayer);
        this.oldBlockText = oldBlockText == null ? "" : oldBlockText;
        this.newBlockText = newBlockText == null ? "" : newBlockText;
        this.finish = finish;
    }

    @Override
    public @NotNull String getOldBlockText() {
        return this.oldBlockText;
    }

    @Override
    public @NotNull String getNewBlockText() {
        return this.newBlockText;
    }

    public void setNewBlockText(@Nullable String newBlockText) {
        this.newBlockText = newBlockText == null ?
                this.oldBlockText : newBlockText;
    }

    public boolean isFinish() {
        return this.finish;
    }
}
