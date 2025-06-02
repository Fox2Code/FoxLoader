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
package com.fox2code.foxloader.loader.early;

import com.fox2code.foxloader.launcher.FoxLauncher;
import net.minecraft.common.CoreConstants;
import net.minecraft.common.util.logging.LogAgent;

/**
 * Initialize a ICoreAccess early to allow game code to work properly during pre-initialization.
 */
public final class EarlyLoader {
    static final LogAgent EARLY_LOG_AGENT = new LogAgent("EARLY", null);
    private EarlyLoader() { throw new AssertionError(); }

    public static void earlyLoad() {
        if (CoreConstants.CORE != null) return;
        if (FoxLauncher.isClient()) {
            CoreConstants.CORE = new Minecraft();
        } else {
            CoreConstants.CORE = new MinecraftServer();
        }
    }
}
