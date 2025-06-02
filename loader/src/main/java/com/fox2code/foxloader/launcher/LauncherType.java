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
package com.fox2code.foxloader.launcher;

/**
 * If you want me to support your custom launcher, please open a new issue!
 */
public enum LauncherType {
    /**
     * Ex: Server ran via --server
     */
    UNKNOWN(false),
    /**
     * BIN is a special broken case that can happen on MultiMC
     * when users lacks the mental capabilities to run a jar file
     */
    BIN(false),
    /**
     * Ex: Dev environment.
     */
    GRADLE(false),
    /**
     * Ex: Vanilla launcher &amp; Pojav launcher
     */
    VANILLA_LIKE(false),
    /**
     * Ex: MultiMC/PolyMC/PrismLauncher
     */
    MMC_LIKE(false);

    public final boolean hasAutoFix;

    LauncherType(boolean hasAutoFix) {
        this.hasAutoFix = hasAutoFix;
    }
}
