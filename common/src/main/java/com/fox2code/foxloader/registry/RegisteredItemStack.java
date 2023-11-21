package com.fox2code.foxloader.registry;

public interface RegisteredItemStack extends GameRegistry.Ingredient {
    default RegisteredItem getRegisteredItem() { throw new RuntimeException(); }

    default int getRegisteredStackSize() { throw new RuntimeException(); }

    default void setRegisteredStackSize(int stackSize) { throw new RuntimeException(); }

    default int getRegisteredDamage() { throw new RuntimeException(); }

    default void setRegisteredDamage(int damage) { throw new RuntimeException(); }

    default String getRegisteredDisplayName() { throw new RuntimeException(); }

    default void setRegisteredDisplayName(String displayName) { throw new RuntimeException(); }

    default int getRegisteredDynamicTextureId() { throw new RuntimeException(); }

    default void setRegisteredDynamicTextureId(int dynamicTextureSlot) { throw new RuntimeException(); }

    default void verifyRegisteredItemStack() { throw new RuntimeException(); }
}
