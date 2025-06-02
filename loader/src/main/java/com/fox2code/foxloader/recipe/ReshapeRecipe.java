package com.fox2code.foxloader.recipe;

import com.mojang.nbt.CompoundTag;
import com.mojang.nbt.Tag;
import net.minecraft.common.entity.inventory.InventoryCrafting;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.recipe.IRecipe;
import net.minecraft.common.recipe.Ingredient;

import java.util.*;

/**
 * Recipe that transfer one item compoundTag data (and optionally damage value) into the result slot.
 */
public class ReshapeRecipe implements IRecipe {
    private final ItemStack output;
    private final Ingredient reshaped;
    private final List<Ingredient> ingredients;

    public ReshapeRecipe(ItemStack output, Ingredient reshaped, List<Ingredient> ingredients) {
        this.output = output;
        this.reshaped = reshaped;
        this.ingredients = ingredients;
    }

    public ItemStack getRecipeOutput() {
        return this.output;
    }

    public boolean matches(InventoryCrafting craftingInventory) {
        ArrayList<Ingredient> ingredients = this.getIngredients();

        for(int w = 0; w < 3; ++w) {
            for(int h = 0; h < 3; ++h) {
                ItemStack out = craftingInventory.func_21103_b(h, w);
                if (out != null) {
                    boolean flag = false;
                    Iterator<Ingredient> ingredientIterator = ingredients.iterator();

                    while(ingredientIterator.hasNext()) {
                        Ingredient ingredient = (Ingredient)ingredientIterator.next();
                        if (ingredient.matchIngredient(out)) {
                            ingredientIterator.remove();
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }
            }
        }

        return ingredients.isEmpty();
    }

    public ItemStack getCraftingResult(InventoryCrafting craftingInventory) {
        ItemStack reshapedItemStack = this.getReshapedItemStack(craftingInventory);
        ItemStack resultItemStack = new ItemStack( // Don't copy compoundTag early
                this.output.getItemID(), this.output.stackSize, this.output.itemDamage);
        if (reshapedItemStack != null) {
            if (resultItemStack.itemDamage == -1) {
                resultItemStack.itemDamage = reshapedItemStack.itemDamage;
            }
            if (reshapedItemStack.compoundTag != null) {
                CompoundTag sourceReshapedItemTag = (CompoundTag)  reshapedItemStack.compoundTag.copy();
                if (this.output.compoundTag != null) {
                    for (Map.Entry<String, Tag> entry : this.output.compoundTag.copyMap().entrySet()) {
                        sourceReshapedItemTag.setTag(entry.getKey(), entry.getValue());
                    }
                }
                resultItemStack.compoundTag = sourceReshapedItemTag;
            }
        }
        if (resultItemStack.itemDamage == -1) {
            resultItemStack.itemDamage = 0;
        }
        if (resultItemStack.compoundTag == null && this.output.compoundTag != null) {
            resultItemStack.compoundTag = (CompoundTag) this.output.compoundTag.copy();
        }
        return resultItemStack;
    }

    public int getRecipeSize() {
        return this.ingredients.size() + 1;
    }

    public ItemStack getReshapedItemStack(InventoryCrafting craftingInventory) {
        for(int w = 0; w < 3; ++w) {
            for(int h = 0; h < 3; ++h) {
                ItemStack out = craftingInventory.func_21103_b(h, w);
                if (this.reshaped.matchIngredient(out)) {
                    return out;
                }
            }
        }
        return null;
    }

    public ArrayList<Ingredient> getIngredients() {
        ArrayList<Ingredient> ingredients = new ArrayList<>(this.ingredients);
        ingredients.add(this.reshaped);
        return ingredients;
    }

    public Ingredient getReshaped() {
        return this.reshaped;
    }

    public List<Ingredient> getIngredientsRaw() {
        return this.ingredients;
    }
}
