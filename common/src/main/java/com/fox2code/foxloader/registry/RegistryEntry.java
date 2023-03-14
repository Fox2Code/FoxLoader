package com.fox2code.foxloader.registry;

public final class RegistryEntry {
    public final short realId, fallbackId;
    public final String name, fallbackDisplayName;

    public RegistryEntry(short realId, short fallbackId, String name, String fallbackDisplayName) {
        this.realId = realId;
        this.fallbackId = fallbackId;
        this.name = name;
        this.fallbackDisplayName = fallbackDisplayName;
    }
}
