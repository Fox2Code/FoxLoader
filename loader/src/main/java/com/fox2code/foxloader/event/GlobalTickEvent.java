package com.fox2code.foxloader.event;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.event.world.WorldTickEvent;

/**
 * Global tick event, only called once per tick and before any {@link WorldTickEvent}
 *
 * This event is called even if there is no world currently loaded
 */
public final class GlobalTickEvent extends Event {
    public static final GlobalTickEvent INSTANCE = new GlobalTickEvent();

    private GlobalTickEvent() {}
}
