package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.player.PlayerEvent;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

/**
 * This event is sent when the player start breaking a block,
 * the event doesn't edit the world directly, but might edit the world indirectly.
 */
public final class PlayerStartBreakBlockEvent extends PlayerEvent implements Event.Cancellable {
    private final int x, y, z, facing;
    private final ItemStack heldItem;

    public PlayerStartBreakBlockEvent(EntityPlayer entityPlayer, int x, int y, int z, int facing, ItemStack heldItem) {
        super(entityPlayer);
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.heldItem = heldItem;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getFacing() {
        return this.facing;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }
}
