package com.fox2code.foxloader.registry;

/**
 * Interface implemented by sided mixins.
 */
public interface RegisteredBlock extends GameRegistry.Ingredient {
    RegisteredItem asRegisteredItem();

    RegisteredItemStack newRegisteredItemStack();

    int getRegisteredBlockId();
}
