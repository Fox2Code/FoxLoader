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
package com.fox2code.foxloader.energy;

import org.jetbrains.annotations.NotNull;

/**
 * Use this if you want to redirect cable to the faces of your blocks, but don't want it to store energy.
 * <p>
 * Can also be used for optimization purposes.
 */
public final class FoxPowerInterfaceNone extends FoxPowerInterface {
    public static final FoxPowerInterfaceNone INSTANCE = new FoxPowerInterfaceNone();

    private FoxPowerInterfaceNone() {}

    @Override
    public long getMaxFoxPowerStorage() {
        return 0;
    }

    @Override
    public long getStoredFoxPower() {
        return 0;
    }

    @Override
    public long getFoxPowerStorageMaxInput() {
        return 0;
    }

    @Override
    public long getFoxPowerStorageMaxOutput() {
        return 0;
    }

    @Override
    public long getFoxPowerStorageStep() {
        return 1;
    }

    @Override
    public long drainFoxPower(long amount) {
        return 0;
    }

    @Override
    public long sendFoxPower(long amount) {
        return 0;
    }

    @Override
    public int getCableSinkPriority() {
        return 0;
    }

    @Override
    public @NotNull FoxPowerType getFoxPowerType() {
        return FoxPowerType.NONE;
    }
}
