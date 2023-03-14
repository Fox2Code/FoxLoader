package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.src.client.player.EntityPlayerSP;

final class ClientSelfTest {
    static void selfTest() {
        try {
            EntityPlayerSP.class.asSubclass(NetworkPlayer.class);
        } catch (ClassCastException e) {
            throw new InternalError("Mixins failed to initialize");
        }
    }
}
