package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.player.PlayerEvent;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

@Event.DelegateEvent
public abstract class PlayerUseItem extends PlayerEvent implements Event.Cancellable {
    private final ItemStack heldItem;

    public PlayerUseItem(EntityPlayer entityPlayer, ItemStack heldItem) {
        super(entityPlayer);
        this.heldItem = heldItem;
    }

    public final ItemStack getHeldItem() {
        return this.heldItem;
    }
}
