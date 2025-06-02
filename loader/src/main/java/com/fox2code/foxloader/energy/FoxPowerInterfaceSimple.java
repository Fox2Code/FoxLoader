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
package com.fox2code.foxloader.energy;

import org.jetbrains.annotations.NotNull;

/**
 * Simplified implementation of {@link FoxPowerInterface}
 */
public abstract class FoxPowerInterfaceSimple extends FoxPowerInterface {
    public abstract long getMaxFoxPowerStorage();

    @Override
    public abstract long getStoredFoxPower();

    public abstract void setStoredFoxPower(long foxPower);

    @Override
    public long getFoxPowerStorageStep() {
        return 1;
    }

    @Override
    public long getFoxPowerStorageMaxOutput() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getFoxPowerStorageMaxInput() {
        return Long.MAX_VALUE;
    }

    @Override
    public long drainFoxPower(long amount) {
        if (amount <= 0) return 0;
        long storedPower = this.getStoredFoxPower();
        long drained = FoxPowerUtils.computeEnergy(amount,
                Math.min(storedPower, this.getFoxPowerStorageMaxOutput()),
                this.getFoxPowerStorageStep());
        if (drained == 0) return 0;
        this.setStoredFoxPower(storedPower - drained);
        this.emitFoxPowerSinkUpdatedHelper(storedPower, storedPower - drained);
        return drained;
    }

    @Override
    public long sendFoxPower(long amount) {
        if (amount <= 0) return 0;
        long storedPower = this.getStoredFoxPower();
        long maxPowerStorage = this.getMaxFoxPowerStorage();
        long sent = FoxPowerUtils.computeEnergy(amount,
                Math.min(maxPowerStorage - storedPower, this.getFoxPowerStorageMaxInput()),
                this.getFoxPowerStorageStep());
        if (sent == 0) return 0;
        this.setStoredFoxPower(storedPower + sent);
        this.emitFoxPowerSinkUpdatedHelper(storedPower, storedPower + sent);
        return sent;
    }

    @Override
    public int getCableSinkPriority() {
        // Don't ask for more energy at 90%~ capacity.
        if (this.capSinkPriority(this.getStoredFoxPower())) {
            return 0;
        }
        return this.getFoxPowerType().cableSinkPriorityDefault;
    }

    public boolean capSinkPriority(long forStoredPower) {
        return ((this.getMaxFoxPowerStorage() / 10) * 9) <= forStoredPower;
    }

    public final void emitFoxPowerSinkUpdatedHelper(long oldFoxPower, long newFoxPower) {
        if (oldFoxPower != newFoxPower && this.capSinkPriority(oldFoxPower) != this.capSinkPriority(newFoxPower)) {
            this.emitFoxPowerSinkUpdated();
        }
    }

    public abstract void emitFoxPowerSinkUpdated();

    @Override
    public abstract @NotNull FoxPowerType getFoxPowerType();
}
