package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.player.PlayerEvent;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

public final class PlayerAttackEntityEvent extends PlayerEvent implements Event.Cancellable {
    private final ItemStack heldItem;
    private final Entity target;

    public PlayerAttackEntityEvent(EntityPlayer entityPlayer, ItemStack heldItem, Entity target) {
        super(entityPlayer);
        this.heldItem = heldItem;
        this.target = target;
    }

    public ItemStack getHeldItem() {
        return this.heldItem;
    }

    public Entity getTarget() {
        return this.target;
    }
}
