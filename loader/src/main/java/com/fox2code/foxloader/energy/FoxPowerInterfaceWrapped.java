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
