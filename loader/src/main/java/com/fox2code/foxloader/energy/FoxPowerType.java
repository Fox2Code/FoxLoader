package com.fox2code.foxloader.energy;

/**
 * May be used by energy transmitters like energy cables to choose which target to discharge energy first on.
 * <p>
 * Recommended priority order for discharging into is [RECEIVER, STORAGE, OTHER, TRANSMITTER, PRODUCER, NONE]
 * <p>
 * Recommended priority order for charging from is [PRODUCER, TRANSMITTER, STORAGE, OTHER, RECEIVER, NONE]
 */
public enum FoxPowerType {
    PRODUCER(1), // Generators
    RECEIVER(256), // Machinery
    STORAGE(128), // Batteries
    TRANSMITTER(1), // Energy cables
    OTHER(128), // Decorations
    NONE(1); // Blocks that redirect cables but don't have energy.

    public final int cableSinkPriorityDefault;

    FoxPowerType(int cableSinkPriorityDefault) {
        this.cableSinkPriorityDefault = cableSinkPriorityDefault;
    }
}
