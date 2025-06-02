package com.fox2code.foxloader.event.lifecycle;

import com.fox2code.foxloader.network.ConnectionType;

public final class LifecycleStopEvent extends LifecycleEvent {
    public static final LifecycleStopEvent SINGLE_PLAYER = new LifecycleStopEvent(ConnectionType.SINGLE_PLAYER);
    public static final LifecycleStopEvent CLIENT_ONLY = new LifecycleStopEvent(ConnectionType.CLIENT_ONLY);
    public static final LifecycleStopEvent SERVER_ONLY = new LifecycleStopEvent(ConnectionType.SERVER_ONLY);

    private LifecycleStopEvent(ConnectionType connectionType) {
        super(connectionType);
    }
}
