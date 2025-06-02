package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.PropertiesConfigParser;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

import java.util.Collection;
import java.util.Map;

final class FoxLoaderSparkServerConfigProvider extends ServerConfigProvider {
    private static final Map<String, ConfigParser> FILES_SERVER =
            ImmutableMap.of("server.properties", PropertiesConfigParser.INSTANCE,
                    "config/foxloader.cfg", FoxLoaderSparkJanksonConfigParser.INSTANCE);
    private static final Map<String, ConfigParser> FILES_CLIENT =
            ImmutableMap.of("config/foxloader.cfg", FoxLoaderSparkJanksonConfigParser.INSTANCE);
    private static final Collection<String> HIDDEN_PATHS;

    public FoxLoaderSparkServerConfigProvider() {
        super(FoxLauncher.isServer() ? FILES_SERVER : FILES_CLIENT, HIDDEN_PATHS);
    }

    static {
        ImmutableSet.Builder<String> hiddenPaths = ImmutableSet.<String>builder()
                .add("server-ip").add("server-port").add("motd").add("resource-pack")
                .add("rcon<dot>port").add("rcon<dot>password").add("level-seed")
                .addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));
        HIDDEN_PATHS = hiddenPaths.build();
    }
}
