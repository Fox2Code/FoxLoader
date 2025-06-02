package com.fox2code.foxloader.event.lifecycle;

import com.fox2code.foxloader.network.ConnectionType;

public final class LifecycleStartEvent extends LifecycleEvent {
    public static final LifecycleStartEvent SINGLE_PLAYER = new LifecycleStartEvent(ConnectionType.SINGLE_PLAYER);
    public static final LifecycleStartEvent CLIENT_ONLY = new LifecycleStartEvent(ConnectionType.CLIENT_ONLY);
    public static final LifecycleStartEvent SERVER_ONLY = new LifecycleStartEvent(ConnectionType.SERVER_ONLY);

    private LifecycleStartEvent(ConnectionType connectionType) {
        super(connectionType);
    }
}
