package com.fox2code.foxloader.registry;

public interface RegisteredEntity {
    default RegisteredWorld getCurrentRegisteredWorld() { throw new RuntimeException(); }

    default double getRegisteredX() { throw new RuntimeException(); }

    default double getRegisteredY() { throw new RuntimeException(); }

    default double getRegisteredZ() { throw new RuntimeException(); }
}
