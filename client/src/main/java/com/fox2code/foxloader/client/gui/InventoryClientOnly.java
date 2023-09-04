package com.fox2code.foxloader.client.gui;

import net.minecraft.src.client.inventory.IInventory;

/**
 * Mark a {@link IInventory} as client side only, and to avoid any network inconsistencies
 */
public interface InventoryClientOnly extends IInventory {
}
