package com.fox2code.foxloader.registry;

/**
 * Interface implemented by sided mixins.
 */
public interface RegisteredItem extends GameRegistry.Ingredient {
    default RegisteredBlock asRegisteredBlock() { throw new RuntimeException(); }

    default RegisteredItemStack newRegisteredItemStack() { throw new RuntimeException(); }

    default int getRegisteredItemId() { throw new RuntimeException(); }

    default int getRegisteredItemMaxStackSize() { throw new RuntimeException(); }

    default void setWorldItemScale(float scale) { throw new RuntimeException(); }

    default float getWorldItemScale() { throw new RuntimeException(); }
}
