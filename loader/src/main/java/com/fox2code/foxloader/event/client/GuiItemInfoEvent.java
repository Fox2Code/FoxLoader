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
package com.fox2code.foxloader.event.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.common.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GuiItemInfoEvent extends GuiScreenEvent {
    private final ItemStack itemStack;
    private List<String> description;

    public GuiItemInfoEvent(GuiScreen guiScreen, ItemStack itemStack, List<String> description) {
        super(guiScreen);
        this.itemStack = itemStack;
        this.description = description;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public List<String> getDescription() {
        return this.description;
    }

    public void setDescription(List<String> description) {
        this.description = description == null ?
                Collections.emptyList() : description;
    }

    public void addDescriptionLine(String line) {
        try {
            this.description.add(line);
        } catch (UnsupportedOperationException e) {
            this.description = new ArrayList<>(this.description);
            this.description.add(line);
        }
    }

    public void addDescriptionLine(int index, String line) {
        int size = this.description.size();
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        try {
            this.description.add(index, line);
        } catch (UnsupportedOperationException e) {
            this.description = new ArrayList<>(this.description);
            this.description.add(index, line);
        }
    }

    public boolean removeDescriptionLine(String line) {
        try {
            return this.description.remove(line);
        } catch (UnsupportedOperationException e) {
            this.description = new ArrayList<>(this.description);
            return this.description.remove(line);
        }
    }
}
