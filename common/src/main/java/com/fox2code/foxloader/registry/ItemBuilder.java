package com.fox2code.foxloader.registry;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

public final class ItemBuilder {
    public int maxStackSize = 64;
    @Nullable
    public String itemName;
    @Nullable
    public RegisteredItem containerItem;
    public int itemBurnTime;
    public byte itemBurnType;
    public int tooltipColor;
    public boolean hideFromCreativeInventory;

    public ItemBuilder() {}

    public ItemBuilder setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    public ItemBuilder setItemName(@Nullable String itemName) {
        this.itemName = itemName;
        return this;
    }

    public ItemBuilder setContainerItem(@Nullable RegisteredItem containerItem) {
        this.containerItem = containerItem;
        this.maxStackSize = 1;
        return this;
    }

    public ItemBuilder setBurnTime(int num) {
        this.itemBurnTime = num;
        this.itemBurnType = 1;
        return this;
    }

    public ItemBuilder setFreezeTime(int num) {
        this.itemBurnTime = num;
        this.itemBurnType = 2;
        return this;
    }

    public ItemBuilder setTooltipColor(int color) {
        if ((color & 0xFF000000) == 0) {
            color |= 0xFF000000;
        }
        this.tooltipColor = color;
        return this;
    }

    public ItemBuilder setTooltipColor(Color color) {
        return this.setTooltipColor(color.getRGB());
    }

    public ItemBuilder hideFromCreativeInventory() {
        this.hideFromCreativeInventory = true;
        return this;
    }
}
