package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.network.NetworkPlayer;

public interface LifecycleListener {
    void onServerStart(NetworkPlayer.ConnectionType connectionType);

    void onServerStop(NetworkPlayer.ConnectionType connectionType);

    static void register(LifecycleListener lifecycleListener) {
        ModLoader.listeners.add(lifecycleListener);
    }
}
