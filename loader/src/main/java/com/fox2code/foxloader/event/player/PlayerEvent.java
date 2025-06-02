package com.fox2code.foxloader.event.player;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class PlayerEvent extends Event {
    private final EntityPlayer entityPlayer;

    protected PlayerEvent(EntityPlayer entityPlayer) {
        this.entityPlayer = Objects.requireNonNull(entityPlayer, "entityPlayer");
    }

    @NotNull
    public final EntityPlayer getEntityPlayer() {
        return this.entityPlayer;
    }
}
