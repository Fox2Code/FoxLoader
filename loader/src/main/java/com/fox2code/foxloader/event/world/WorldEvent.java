package com.fox2code.foxloader.event.world;

import com.fox2code.foxevents.Event;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class WorldEvent extends Event {
    private final World world;

    protected WorldEvent(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    @NotNull public World getWorld() {
        return this.world;
    }
}
