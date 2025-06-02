package com.fox2code.foxloader.event.world;

import com.fox2code.foxevents.Event;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.world.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Event.DelegateEvent
public final class WorldExplosionEvent extends WorldMultiBlockChange {
    private final Explosion explosion;

    public WorldExplosionEvent(Explosion explosion, Collection<BlockChange> blockChangeList) {
        super(explosion.worldObj, blockChangeList);
        this.explosion = explosion;
    }

    @Override
    public @Nullable Entity getEntitySource() {
        return this.explosion.exploder;
    }

    @NotNull
    public Explosion getExplosion() {
        return this.explosion;
    }
}
