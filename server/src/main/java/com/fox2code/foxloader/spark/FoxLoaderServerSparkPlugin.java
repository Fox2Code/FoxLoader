package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.loader.ServerMod;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

import java.util.stream.Stream;

public class FoxLoaderServerSparkPlugin extends FoxLoaderSparkPlugin {
    public FoxLoaderServerSparkPlugin() {
        super(PlatformInfo.Type.SERVER);
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return ServerMod.getOnlineNetworkPlayers().stream().map(FoxLoaderSparkCommandSender::new);
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new FoxLoaderSparkPlayerPingProvider();
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new FoxLoaderSparkServerConfigProvider();
    }
}
