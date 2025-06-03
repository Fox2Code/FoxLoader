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
package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.recipe.PrepareRecipeEvent;
import net.minecraft.common.entity.inventory.InventoryCrafting;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.recipe.IRecipe;

public final class InternalRecipeHooks {
    private static final EventHolder<PrepareRecipeEvent> PREPARE_RECIPE_EVENT =
            EventHolder.getHolderFromEvent(PrepareRecipeEvent.class);

    private InternalRecipeHooks() {}

    public static ItemStack onWorkbenchRecipe(ItemStack itemStack, InventoryCrafting inventoryCrafting) {
        if (PREPARE_RECIPE_EVENT.isEmpty()) {
            return itemStack;
        }
        PrepareRecipeEvent prepareRecipeEvent =
                new PrepareRecipeEvent(null, inventoryCrafting, itemStack);
        prepareRecipeEvent.callEvent();
        return prepareRecipeEvent.getEffectiveResult();
    }

    public static ItemStack onWorkbenchRecipe(IRecipe recipe, InventoryCrafting inventoryCrafting) {
        if (PREPARE_RECIPE_EVENT.isEmpty()) {
            return recipe.getCraftingResult(inventoryCrafting);
        }
        PrepareRecipeEvent prepareRecipeEvent =
                new PrepareRecipeEvent(recipe, inventoryCrafting,
                        recipe.getCraftingResult(inventoryCrafting));
        prepareRecipeEvent.callEvent();
        return prepareRecipeEvent.getEffectiveResult();
    }
}
