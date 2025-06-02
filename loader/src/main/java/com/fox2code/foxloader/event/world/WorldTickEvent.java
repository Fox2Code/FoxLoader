package com.fox2code.foxloader.event.world;

import net.minecraft.common.world.World;

/**
 * Called when a world is ticking, can be called multiples
 * times per tick if multiple worlds are loaded.
 */
public final class WorldTickEvent extends WorldEvent {
    public WorldTickEvent(World world) {
        super(world);
    }
}
