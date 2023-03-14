package com.fox2code.foxloader.spark;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.PropertiesConfigParser;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

import java.util.Collection;
import java.util.Map;

public class FoxLoaderSparkServerConfigProvider extends ServerConfigProvider {
    private static final Map<String, ConfigParser> FILES =
            ImmutableMap.of("server.properties", PropertiesConfigParser.INSTANCE);
    private static final Collection<String> HIDDEN_PATHS;

    public FoxLoaderSparkServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    static {
        ImmutableSet.Builder<String> hiddenPaths = ImmutableSet.<String>builder()
                .add("server-ip").add("motd").add("resource-pack")
                .add("rcon<dot>password").add("level-seed")
                .addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));
        HIDDEN_PATHS = hiddenPaths.build();
    }
}
