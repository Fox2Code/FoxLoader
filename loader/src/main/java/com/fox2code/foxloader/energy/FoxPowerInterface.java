package com.fox2code.foxloader.energy;

import org.jetbrains.annotations.NotNull;

public abstract class FoxPowerInterface {
    public abstract long getMaxFoxPowerStorage();

    public abstract long getStoredFoxPower();

    /**
     * @return minimum step of this energy source
     */
    public abstract long getFoxPowerStorageStep();

    /**
     * @return maximum energy per tick this interface can provide
     */
    public abstract long getFoxPowerStorageMaxOutput();

    /**
     * @return maximum energy per tick this interface can receive
     */
    public abstract long getFoxPowerStorageMaxInput();

    public abstract long drainFoxPower(long amount);

    public abstract long sendFoxPower(long amount);

    /**
     * @return sink priority, used by power cables to help with the cable power distribution algorithm,
     * should never be greater than {@link FoxPowerUtils#getMaxSinkPriorityValue()}.
     */
    public abstract int getCableSinkPriority();

    @NotNull public abstract FoxPowerType getFoxPowerType();
}
