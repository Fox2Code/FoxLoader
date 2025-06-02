/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
