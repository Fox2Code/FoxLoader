package com.fox2code.foxloader.registry;

/**
 * Interface implemented by sided mixins.
 */
public interface RegisteredItem extends GameRegistry.Ingredient {
    RegisteredBlock asRegisteredBlock();

    RegisteredItemStack newRegisteredItemStack();

    int getRegisteredItemId();

    int getRegisteredItemMaxStackSize();
}
