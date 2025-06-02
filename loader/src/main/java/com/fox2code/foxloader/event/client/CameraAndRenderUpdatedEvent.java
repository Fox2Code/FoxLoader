package com.fox2code.foxloader.event.client;

import com.fox2code.foxevents.Event;

public final class CameraAndRenderUpdatedEvent extends Event {
    public static final CameraAndRenderUpdatedEvent INSTANCE = new CameraAndRenderUpdatedEvent();

    private float deltaTicks;

    private CameraAndRenderUpdatedEvent() {}

    public float getDeltaTicks() {
        return this.deltaTicks;
    }

    public void callEvent(float deltaTicks) {
        this.deltaTicks = deltaTicks;
        this.callEvent();
    }
}
