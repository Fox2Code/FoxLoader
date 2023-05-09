package com.fox2code.foxloader.registry;

public interface RegisteredEntityItem extends RegisteredEntity {
    default RegisteredItemStack getRegisteredItemStack() { throw new RuntimeException(); }

    default void setRegisteredItemStack(RegisteredItemStack registeredItemStack) { throw new RuntimeException(); }
}
