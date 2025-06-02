package com.fox2code.foxloader.spark;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import me.lucko.spark.lib.adventure.text.Component;
import me.lucko.spark.lib.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.common.command.ICommandListener;

import java.util.UUID;

final class FoxLoaderSparkCommandSender extends AbstractCommandSender<ICommandListener> {
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacy('§');
    private final boolean absolute; // <- Absolute is used for "/sparkclient" command.

    public FoxLoaderSparkCommandSender(ICommandListener commandListener) {
        this(commandListener, false);
    }

    public FoxLoaderSparkCommandSender(ICommandListener commandListener, boolean absolute) {
        super(commandListener);
        this.absolute = absolute;
    }

    @Override
    public String getName() {
        return this.delegate.getUsername();
    }

    @Override
    public UUID getUniqueId() {
        // Unsupported in ReIndev
        return null;
    }

    @Override
    public void sendMessage(Component component) {
        this.delegate.log(LEGACY_COMPONENT_SERIALIZER.serialize(component));
    }

    @Override
    public boolean hasPermission(String s) {
        return this.absolute || this.delegate.isConsole() ||
                this.delegate.isOp(this.delegate.getUsername());
    }
}
