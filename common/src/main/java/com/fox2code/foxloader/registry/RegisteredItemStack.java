package com.fox2code.foxloader.registry;

public interface RegisteredItemStack extends GameRegistry.Ingredient {
    float MINIMUM_WORLD_ITEM_SCALE = 0.25F;

    default RegisteredItem getRegisteredItem() { throw new RuntimeException(); }

    default int getRegisteredStackSize() { throw new RuntimeException(); }

    default void setRegisteredStackSize(int stackSize) { throw new RuntimeException(); }

    default int getRegisteredDamage() { throw new RuntimeException(); }

    default void setRegisteredDamage(int damage) { throw new RuntimeException(); }

    default String getRegisteredDisplayName() { throw new RuntimeException(); }

    default void setRegisteredDisplayName(String displayName) { throw new RuntimeException(); }

    default boolean hasCustomWorldItemScale() { throw new RuntimeException(); }

    default void resetWorldItemScale() { throw new RuntimeException(); }

    // Note: Will only be visible on client with FoxLoader
    default void setWorldItemScale(float scale) { throw new RuntimeException(); }

    default float getWorldItemScale() { throw new RuntimeException(); }

    default void verifyRegisteredItemStack() { throw new RuntimeException(); }
}
