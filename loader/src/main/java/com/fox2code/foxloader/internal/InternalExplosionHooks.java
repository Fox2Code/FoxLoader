package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.world.WorldExplosionEvent;
import com.fox2code.foxloader.event.world.WorldMultiBlockChange;
import net.minecraft.common.world.Explosion;

import java.util.Collection;

public final class InternalExplosionHooks {
    private static final EventHolder<WorldExplosionEvent> WORLD_EXPLOSION_EVENT =
            EventHolder.getHolderFromEvent(WorldExplosionEvent.class);

    private InternalExplosionHooks() {}

    public static boolean onSendExplosionB(
            Explosion explosion, Collection<WorldMultiBlockChange.BlockChange> changes) {
        if (WORLD_EXPLOSION_EVENT.isEmpty() || changes.isEmpty()) return false;
        WorldExplosionEvent worldExplosionEvent = new WorldExplosionEvent(explosion, changes);
        WORLD_EXPLOSION_EVENT.callEvent(worldExplosionEvent);
        changes.removeIf(blockChange -> blockChange.cancelled);
        return worldExplosionEvent.isCancelled();
    }
}
