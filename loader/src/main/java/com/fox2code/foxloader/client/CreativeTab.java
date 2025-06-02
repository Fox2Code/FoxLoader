package com.fox2code.foxloader.client;

import com.fox2code.foxloader.registry.GameRegistry;
import net.minecraft.client.gui.creative.*;
import net.minecraft.common.block.Block;
import net.minecraft.common.item.Item;
import net.minecraft.common.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public enum CreativeTab {
    BUILDING_BLOCKS(() -> new AccessFunctions(
            CreativeTabBuildingBlocks::getGlobalItemList, CreativeTabBuildingBlocks::addToSlot)),
    NATURAL_BLOCKS(() -> new AccessFunctions(
            CreativeTabNaturalBlocks::getGlobalItemList, CreativeTabNaturalBlocks::addToSlot)),
    COLORED_BLOCKS(() -> new AccessFunctions(
                    CreativeTabColoredBlocks::getGlobalItemList, CreativeTabColoredBlocks::addToSlot)),
    FUNCTIONAL_BLOCKS(() -> new AccessFunctions(
                    CreativeTabFunctionalBlocks::getGlobalItemList, CreativeTabFunctionalBlocks::addToSlot)),
    MECHANICAL_BLOCKS(() -> new AccessFunctions(
                    CreativeTabMechanicalBlocks::getGlobalItemList, CreativeTabMechanicalBlocks::addToSlot)),
    TOOLS(() -> new AccessFunctions(
                    CreativeTabTools::getGlobalItemList, CreativeTabTools::addToSlot)),
    FOODSTUFF(() -> new AccessFunctions(
                    CreativeTabFoodstuffs::getGlobalItemList, CreativeTabFoodstuffs::addToSlot)),
    POTIONS(() -> new AccessFunctions(
                    CreativeTabPotions::getGlobalItemList, CreativeTabPotions::addToSlot)),
    MISCELLANEOUS(() -> new AccessFunctions(
                    CreativeTabMiscellaneous::getGlobalItemList, CreativeTabMiscellaneous::addToSlot)),
    SPAWN_EGGS(() -> new AccessFunctions(
            CreativeTabSpawnEggs::getGlobalItemList, CreativeTabSpawnEggs::addToSlot));

    private final Supplier<AccessFunctions> accessFunctionsSupplier;
    private AccessFunctions accessFunctions;

    CreativeTab(Supplier<AccessFunctions> accessFunctionsSupplier) {
        this.accessFunctionsSupplier = accessFunctionsSupplier;
        this.accessFunctions = null;
    }

    private AccessFunctions getAccessFunctions() {
        AccessFunctions accessFunctions = this.accessFunctions;
        if (accessFunctions != null) {
            return accessFunctions;
        }
        if (!GameRegistry.isInitialized()) {
            throw new IllegalStateException("GameRegistry not initialized");
        }
        accessFunctions = this.accessFunctionsSupplier.get();
        Objects.requireNonNull(accessFunctions, "accessFunctions");
        this.accessFunctions = accessFunctions;
        return accessFunctions;
    }

    public List<ItemStack> getItems() {
        return this.getAccessFunctions().tabItems.get();
    }

    public void addToCreativeTab(ItemStack itemStack) {
        this.getAccessFunctions().addToCreativeTab.accept(itemStack);
    }

    public void addToCreativeTab(Block block) {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        block.populateCreativeInventory(itemStacks);
        itemStacks.forEach(this.getAccessFunctions().addToCreativeTab);
    }

    public void addToCreativeTab(Item item) {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        item.populateCreativeInventory(itemStacks);
        itemStacks.forEach(this.getAccessFunctions().addToCreativeTab);
    }

    private static final class AccessFunctions {
        private final Supplier<List<ItemStack>> tabItems;
        private final Consumer<ItemStack> addToCreativeTab;

        private AccessFunctions(Supplier<List<ItemStack>> tabItems, Consumer<ItemStack> addToCreativeTab) {
            this.tabItems = tabItems;
            this.addToCreativeTab = addToCreativeTab;
        }
    }
}
