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
