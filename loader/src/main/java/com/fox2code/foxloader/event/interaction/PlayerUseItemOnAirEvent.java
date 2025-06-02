package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;

@Event.DelegateEvent
public final class PlayerUseItemOnAirEvent extends PlayerUseItem {
    public PlayerUseItemOnAirEvent(EntityPlayer entityPlayer, ItemStack heldItem) {
        super(entityPlayer, heldItem);
    }
}
