package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements RegisteredItemStack {
    @Shadow
    public int stackSize;
    @Shadow int itemDamage;

    @Shadow public abstract Item getItem();
    @Shadow public abstract String getDisplayName();
    @Shadow public abstract void setItemName(String par1Str);
    @Shadow public abstract boolean hasDisplayName();

    @Override
    public RegisteredItem getRegisteredItem() {
        return (RegisteredItem) this.getItem();
    }

    @Override
    public int getRegisteredStackSize() {
        return this.stackSize;
    }

    @Override
    public void setRegisteredStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    @Override
    public int getRegisteredDamage() {
        return this.itemDamage;
    }

    @Override
    public void setRegisteredDamage(int damage) {
        this.itemDamage = damage;
    }

    @Override
    public String getRegisteredDisplayName() {
        return this.hasDisplayName() ? this.getDisplayName() : null;
    }

    @Override
    public void setRegisteredDisplayName(String displayName) {
        this.setItemName(displayName);
    }
}
