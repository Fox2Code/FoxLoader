package com.fox2code.foxloader.event.lifecycle;

import com.fox2code.foxevents.Event;
import com.fox2code.foxloader.network.ConnectionType;

public abstract class LifecycleEvent extends Event {
    private final ConnectionType connectionType;

    protected LifecycleEvent(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public ConnectionType getConnectionType() {
        return this.connectionType;
    }
}
