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
