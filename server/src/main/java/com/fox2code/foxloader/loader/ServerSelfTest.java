package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.src.game.entity.player.EntityPlayerMP;

/**
 * The test needs to be in a separate class to avoid class loading issues.
 */
final class ServerSelfTest {
    static void selfTest() {
        try {
            EntityPlayerMP.class.asSubclass(NetworkPlayer.class);
        } catch (ClassCastException e) {
            throw new InternalError("Mixins failed to initialize");
        }
    }
}
