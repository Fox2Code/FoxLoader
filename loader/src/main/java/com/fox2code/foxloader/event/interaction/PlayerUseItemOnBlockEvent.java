package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

@Event.DelegateEvent
public final class PlayerUseItemOnBlockEvent extends PlayerUseItem {
    private final int x, y, z, facing;
    private final float xOffset, yOffset, zOffset;

    public PlayerUseItemOnBlockEvent(EntityPlayer entityPlayer, ItemStack heldItem,
                                     int x, int y, int z, int facing,
                                     float xOffset, float yOffset, float zOffset) {
        super(entityPlayer, heldItem);
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
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

    public float getXOffset() {
        return this.xOffset;
    }

    public float getYOffset() {
        return this.yOffset;
    }

    public float getZOffset() {
        return this.zOffset;
    }
}