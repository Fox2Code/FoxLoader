package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.loader.ClientMod;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;

import java.util.stream.Stream;

public class FoxLoaderClientSparkPlugin extends FoxLoaderSparkPlugin {
    public FoxLoaderClientSparkPlugin() {
        super(PlatformInfo.Type.CLIENT);
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return Stream.of(new FoxLoaderSparkCommandSender(ClientMod.getLocalNetworkPlayer(), true));
    }
}
