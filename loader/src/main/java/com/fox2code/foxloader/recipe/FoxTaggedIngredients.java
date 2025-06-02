package com.fox2code.foxloader.recipe;

import net.minecraft.common.item.ItemStack;
import net.minecraft.common.item.Items;
import net.minecraft.common.recipe.TaggedIngredient;
import net.minecraft.common.recipe.TaggedIngredients;

public final class FoxTaggedIngredients {
    public static final TaggedIngredient DYE_ANY = TaggedIngredients.get("dye_any");
    public static final TaggedIngredient DYE_RED = TaggedIngredients.get("dye_red");
    public static final TaggedIngredient DYE_ORANGE = TaggedIngredients.get("dye_orange");
    public static final TaggedIngredient DYE_YELLOW = TaggedIngredients.get("dye_yellow");
    public static final TaggedIngredient DYE_LIME = TaggedIngredients.get("dye_lime");
    public static final TaggedIngredient DYE_GREEN = TaggedIngredients.get("dye_green");
    public static final TaggedIngredient DYE_LIGHT_BLUE = TaggedIngredients.get("dye_light_blue");
    public static final TaggedIngredient DYE_CYAN = TaggedIngredients.get("dye_cyan");
    public static final TaggedIngredient DYE_BLUE = TaggedIngredients.get("dye_blue");
    public static final TaggedIngredient DYE_INDIGO = TaggedIngredients.get("dye_indigo");
    public static final TaggedIngredient DYE_PURPLE = TaggedIngredients.get("dye_purple");
    public static final TaggedIngredient DYE_PINK = TaggedIngredients.get("dye_pink");
    public static final TaggedIngredient DYE_BROWN = TaggedIngredients.get("dye_brown");
    public static final TaggedIngredient DYE_BLACK = TaggedIngredients.get("dye_black");
    public static final TaggedIngredient DYE_GRAY = TaggedIngredients.get("dye_gray");
    public static final TaggedIngredient DYE_LIGHT_GRAY = TaggedIngredients.get("dye_light_gray");
    public static final TaggedIngredient DYE_WHITE = TaggedIngredients.get("dye_white");
    public static final TaggedIngredient[] DYES = new TaggedIngredient[]{
            DYE_RED, DYE_ORANGE, DYE_YELLOW, DYE_LIME, DYE_GREEN, DYE_LIGHT_BLUE, DYE_CYAN, DYE_BLUE,
            DYE_INDIGO, DYE_PURPLE, DYE_PINK, DYE_BROWN, DYE_BLACK, DYE_GRAY, DYE_LIGHT_GRAY, DYE_WHITE
    };

    static {
        for (int i = 0; i < DYES.length; i++) {
            TaggedIngredient dye = DYES[i];
            dye.addIngredient(new ItemStack(Items.DYE_POWDER, 1, i));
            DYE_ANY.addIngredient(dye);
        }
    }

    private FoxTaggedIngredients() {}
}
