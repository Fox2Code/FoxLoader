package com.fox2code.foxloader.registry.missing;

import net.minecraft.common.item.Item;

public class MissingItem extends Item {
    public MissingItem(String name) {
        super(name);
        this.setMaxStackSize(1);
        this.addDescription(new MissingItemDesc());
        this.hideFromCreativeMenu();
    }
}
