package com.fox2code.foxloader.registry;

public interface RegisteredItemStack extends GameRegistry.Ingredient {
    RegisteredItem getRegisteredItem();

    int getRegisteredStackSize();

    void setRegisteredStackSize(int stackSize);

    int getRegisteredDamage();

    void setRegisteredDamage(int damage);

    String getRegisteredDisplayName();

    void setRegisteredDisplayName(String displayName);
}
