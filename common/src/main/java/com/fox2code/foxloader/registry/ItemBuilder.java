package com.fox2code.foxloader.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public final class ItemBuilder {
    public ItemProvider gameItemProvider;
    public int maxStackSize = 64;
    @Nullable
    public String itemName;
    @Nullable
    public RegisteredItem containerItem;
    public int itemBurnTime;
    public byte itemBurnType;
    public int tooltipColor;
    public float worldItemScale;
    public boolean hideFromCreativeInventory;

    public ItemBuilder() {}

    @FunctionalInterface
    public interface ItemProvider {
        /**
         * @param id item id of the item
         * @param itemBuilder item builder used to register this item
         * @param block if the item is an item block, this would be the block associated with the item
         * @return the new block to be registered
         */
        RegisteredItem provide(int id, @NotNull ItemBuilder itemBuilder,@Nullable RegisteredBlock block) throws ReflectiveOperationException;
    }

    public ItemBuilder setGameItemProvider(ItemProvider gameItemProvider) {
        this.gameItemProvider = gameItemProvider;
        return this;
    }

    @Deprecated // Use setGameItemProvider instead
    public ItemBuilder setGameItemSource(final Class<? extends RegisteredItem> gameItemSource) {
        // Note: This code mimic old code, with all it's bugs and glory
        this.gameItemProvider = (id, builder, block) -> {
            if (block != null) {
                try {
                    //noinspection JavaReflectionInvocation
                    return gameItemSource.getDeclaredConstructor(
                            // Should work for both client and server side
                            Class.forName("net.minecraft.src.game.block.Block"), int.class)
                            .newInstance(block, id /* - GameRegistry.PARAM_ITEM_ID_DIFF */);
                } catch (NoSuchMethodException e) {
                    try {
                        return gameItemSource.getDeclaredConstructor(int.class)
                                .newInstance(id /* - GameRegistry.PARAM_ITEM_ID_DIFF */);
                    } catch (NoSuchMethodException ignored) {
                        throw e;
                    }
                }
            }
            return gameItemSource.getDeclaredConstructor(int.class)
                    .newInstance(id /* - GameRegistry.PARAM_ITEM_ID_DIFF */);
        };
        return this;
    }

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

    public ItemBuilder setWorldItemScale(float worldItemScale) {
        this.worldItemScale = worldItemScale;
        return this;
    }

    public ItemBuilder hideFromCreativeInventory() {
        this.hideFromCreativeInventory = true;
        return this;
    }
}
