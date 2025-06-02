package com.fox2code.foxloader.spark;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.EntityPlayerMP;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FoxLoaderSparkPlayerPingProvider implements PlayerPingProvider {
    @Override
    public Map<String, Integer> poll() {
        List<EntityPlayerMP> networkPlayers =
                MinecraftServer.getInstance().configManager.playerEntities;
        if (networkPlayers.isEmpty()) return Collections.emptyMap();
        HashMap<String, Integer> pings = new HashMap<>(networkPlayers.size());
        for (EntityPlayerMP entityPlayerMP : networkPlayers) {
            pings.put(entityPlayerMP.username, entityPlayerMP.ping);
        }
        return pings;
    }
}
