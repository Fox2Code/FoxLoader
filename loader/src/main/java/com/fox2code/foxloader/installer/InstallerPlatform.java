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
package com.fox2code.foxloader.installer;

enum InstallerPlatform {
    DEFAULT(false, false, false),
    FULLSCREEN_TEST(true, true, false),
    POJAV_LAUNCHER(false, true, true, true, "Pojav");

    public final boolean fullscreen;
    public final boolean fullscreenLayout;
    public final boolean specialLauncher;
    public final boolean doSilentInstall;
    public final String platformName;

    InstallerPlatform(boolean fullscreen, boolean fullscreenLayout,
                      boolean specialLauncher) {
        this(fullscreen, fullscreenLayout, specialLauncher, false, "Minecraft");
    }

    InstallerPlatform(boolean fullscreen, boolean fullscreenLayout,
                      boolean specialLauncher, boolean doSilentInstall,
                      String platformName) {
        if (fullscreen && !fullscreenLayout) {
            throw new IllegalArgumentException("Fullscreen layout required to allow fullscreen frame!");
        }
        this.fullscreen = fullscreen;
        this.fullscreenLayout = fullscreenLayout;
        this.specialLauncher = specialLauncher;
        this.doSilentInstall = doSilentInstall;
        this.platformName = platformName;
    }
}
