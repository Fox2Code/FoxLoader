package com.fox2code.foxloader.registry;

/**
 * Interface implemented by sided mixins.
 */
public interface RegisteredBlock extends GameRegistry.Ingredient {
    default RegisteredItem asRegisteredItem() { throw new RuntimeException(); }

    default RegisteredItemStack newRegisteredItemStack() { throw new RuntimeException(); }

    default int getRegisteredBlockId() { throw new RuntimeException(); }
}
