/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
