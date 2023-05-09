package com.fox2code.foxloader.registry;

public interface RegisteredEntity {
    default RegisteredWorld getCurrentRegisteredWorld() { throw new RuntimeException(); }

    default double getRegisteredX() { throw new RuntimeException(); }

    default double getRegisteredY() { throw new RuntimeException(); }

    default double getRegisteredZ() { throw new RuntimeException(); }

    default void teleportRegistered(double x, double y, double z) { throw new RuntimeException(); }

    default void killRegistered() { throw new RuntimeException(); }

    default RegisteredEntity getRegisteredRidding() { throw new RuntimeException(); }

    default RegisteredEntity getRegisteredRiddenBy() { throw new RuntimeException(); }
}
