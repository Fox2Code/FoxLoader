/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.event.recipe;

import com.fox2code.foxevents.Event;
import net.minecraft.common.block.container.Container;
import net.minecraft.common.entity.inventory.InventoryCrafting;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.recipe.IRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Called when a recipe is being prepared.
 */
public final class PrepareRecipeEvent extends Event implements Event.Cancellable {
    private final IRecipe recipe;
    private final InventoryCrafting inventory;
    private ItemStack result;

    public PrepareRecipeEvent(@Nullable IRecipe recipe,@NotNull InventoryCrafting inventory,@Nullable ItemStack result) {
        this.recipe = recipe;
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.result = result;
    }

    @Nullable public IRecipe getRecipe() {
        return this.recipe;
    }

    @NotNull public InventoryCrafting getInventory() {
        return this.inventory;
    }

    @Nullable public ItemStack getResult() {
        return this.result;
    }

    public void setResult(@Nullable ItemStack result) {
        this.result = result;
    }

    @Nullable public ItemStack getEffectiveResult() {
        return this.isCancelled() ? null : this.result;
    }

    /**
     * @return the current viewers of the crafting process, can be an empty set, especially on automated machines.
     */
    @NotNull public Set<EntityPlayer> getRecipeViewers() {
        Container container = this.inventory.getCraftingContainer();
        Set<EntityPlayer> entityPlayers;
        if (container == null || (entityPlayers = container.getActiveViewers()) == null) {
            return Collections.emptySet();
        }
        return entityPlayers;
    }
}
