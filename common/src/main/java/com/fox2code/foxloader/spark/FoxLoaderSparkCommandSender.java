package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.network.NetworkPlayer;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import me.lucko.spark.lib.adventure.text.Component;
import me.lucko.spark.lib.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class FoxLoaderSparkCommandSender extends AbstractCommandSender<NetworkPlayer> {
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacy('§');

    public FoxLoaderSparkCommandSender(NetworkPlayer delegate) {
        super(delegate);
    }

    @Override
    public String getName() {
        return this.delegate == null ? "Console" : this.delegate.getPlayerName();
    }

    @Override
    public UUID getUniqueId() {
        // Unsupported in ReIndev
        return null;
    }

    @Override
    public void sendMessage(Component component) {
        this.delegate.displayChatMessage(LEGACY_COMPONENT_SERIALIZER.serialize(component));
    }

    @Override
    public boolean hasPermission(String s) {
        return this.delegate.isOperator();
    }
}
