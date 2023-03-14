package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.loader.ServerMod;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import net.minecraft.src.game.entity.player.EntityPlayerMP;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoxLoaderSparkPlayerPingProvider implements PlayerPingProvider {
    @Override
    public Map<String, Integer> poll() {
        List<EntityPlayerMP> networkPlayers = ServerMod.getOnlinePlayers();
        if (networkPlayers.isEmpty()) return Collections.emptyMap();
        HashMap<String, Integer> pings = new HashMap<>(networkPlayers.size());
        for (EntityPlayerMP entityPlayerMP : networkPlayers) {
            pings.put(entityPlayerMP.username, entityPlayerMP.ping);
        }
        return pings;
    }
}
