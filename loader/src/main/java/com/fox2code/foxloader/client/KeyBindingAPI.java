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
package com.fox2code.foxloader.client;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.client.util.KeyBinding;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public final class KeyBindingAPI {
    private static final ArrayList<KeyBinding> registeredKeyBindings = new ArrayList<>();
    private static boolean loaded;

    private KeyBindingAPI() { throw new AssertionError(); }

    public static void registerKeyBinding(@NotNull KeyBinding keyBinding) {
        if (loaded) throw new IllegalStateException("Options are already loaded");
        registeredKeyBindings.add(keyBinding);
    }

    public static class Internal {
        public static KeyBinding[] inject(KeyBinding[] keyBindings) {
            if (!ModLoader.areAllModsLoaded())
                throw new IllegalStateException("Mods didn't finished to load!");
            loaded = true;
            if (registeredKeyBindings.isEmpty())
                return keyBindings;
            KeyBinding[] newKeyBindings = Arrays.copyOf(keyBindings,
                    keyBindings.length + registeredKeyBindings.size());
            System.arraycopy(registeredKeyBindings.toArray(new KeyBinding[0]), 0,
                    newKeyBindings, keyBindings.length, registeredKeyBindings.size());
            return newKeyBindings;
        }
    }
}