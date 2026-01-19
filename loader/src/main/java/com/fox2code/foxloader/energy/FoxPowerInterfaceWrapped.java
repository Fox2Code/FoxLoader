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
 * To use when exposing an existing power interface with differing capabilities from the main power interface
 */
public abstract class FoxPowerInterfaceWrapped extends FoxPowerInterface {
    private final FoxPowerInterface wrapped;

    public FoxPowerInterfaceWrapped(FoxPowerInterface wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public long getMaxFoxPowerStorage() {
        return this.wrapped.getMaxFoxPowerStorage();
    }

    @Override
    public long getStoredFoxPower() {
        return this.wrapped.getStoredFoxPower();
    }

    @Override
    public long getFoxPowerStorageStep() {
        return this.wrapped.getFoxPowerStorageStep();
    }

    @Override
    public long getFoxPowerStorageMaxOutput() {
        return this.wrapped.getFoxPowerStorageMaxOutput();
    }

    @Override
    public long getFoxPowerStorageMaxInput() {
        return this.wrapped.getFoxPowerStorageMaxInput();
    }

    @Override
    public long drainFoxPower(long amount) {
        return this.wrapped.drainFoxPower(amount);
    }

    @Override
    public long sendFoxPower(long amount) {
        return this.wrapped.sendFoxPower(amount);
    }

    @Override
    public int getCableSinkPriority() {
        return this.wrapped.getCableSinkPriority();
    }

    @Override
    public @NotNull FoxPowerType getFoxPowerType() {
        return this.wrapped.getFoxPowerType();
    }

    public final FoxPowerInterface getWrapped() {
        return this.wrapped;
    }
}
