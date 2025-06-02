package com.fox2code.foxloader.event.interaction;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.world.WorldChangeEvent;
import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Event.DelegateEvent
public final class PlayerBreakBlockEvent extends WorldChangeEvent.SingleBlockChange {
    private final EntityPlayer entityPlayer;
    private final ItemStack heldItem;

    public PlayerBreakBlockEvent(EntityPlayer entityPlayer, int x, int y, int z, ItemStack heldItem) {
        super(entityPlayer.worldObj, x, y, z);
        this.entityPlayer = entityPlayer;
        this.heldItem = heldItem;
    }

    @Override
    public @NotNull EntityPlayer getEntitySource() {
        return this.entityPlayer;
    }

    public ItemStack getHeldItem() {
        return this.heldItem;
    }
}
