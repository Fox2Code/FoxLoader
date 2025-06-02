package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

@Event.DelegateEvent
public final class PlayerUseItemOnEntityEvent extends PlayerUseItem {
    private final Entity target;

    public PlayerUseItemOnEntityEvent(EntityPlayer entityPlayer, ItemStack heldItem, Entity target) {
        super(entityPlayer, heldItem);
        this.target = target;
    }

    public Entity getTarget() {
        return this.target;
    }
}
