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

import com.fox2code.foxloader.event.FoxLoaderEvents;
import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.common.entity.player.EntityPlayer;

import java.util.Objects;

public abstract class PlayerSelectionProvider {
    private static boolean initialized;
    private static PlayerSelectionProvider IMPLEMENTATION;

    public static PlayerSelectionProvider getImplementation() {
        if (!ModLoader.areAllModsFullyLoaded()) {
            throw new IllegalStateException("Cannot get the current implementation before the game loaded.");
        }
        return IMPLEMENTATION;
    }

    public static void setImplementation(PlayerSelectionProvider implementation) {
        if (ModLoader.areAllModsFullyLoaded()) {
            throw new IllegalStateException("Cannot change the implementation after the game has already started.");
        }
        Objects.requireNonNull(implementation);
        if (IMPLEMENTATION != null && IMPLEMENTATION != implementation) {
            throw new IllegalStateException("An alternative implementation already has been registered");
        }
        IMPLEMENTATION = implementation;
    }

    public static void initialize() {
        if (initialized || !ModLoader.areAllModsFullyLoaded()) {
            throw new IllegalStateException("Only FoxLoader can call \"initialize()\"");
        }
        initialized = true;
        if (IMPLEMENTATION == null) {
            IMPLEMENTATION = new PlayerSelectionProviderFallback();
            FoxLoaderEvents.INSTANCE.registerEvents(IMPLEMENTATION);
        }
    }

    public abstract PlayerSelection getPlayerSelection(EntityPlayer entityPlayer);
}
