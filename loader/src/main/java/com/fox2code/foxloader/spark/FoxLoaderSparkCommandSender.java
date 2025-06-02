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
