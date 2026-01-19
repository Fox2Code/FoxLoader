/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.loader;

import com.fox2code.foxevents.Event;
import com.fox2code.foxevents.EventCallback;
import com.fox2code.foxloader.config.ConfigIO;
import net.minecraft.common.networking.NetworkManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class Mod {
    ModContainer modContainer;

    public Mod() {}

    @NotNull public final ModContainer getModContainer() {
        return Objects.requireNonNull(modContainer == null ? ModContainer.tmp : modContainer);
    }

    @NotNull public final Logger getLogger() {
        return this.getModContainer().getLogger();
    }

    @NotNull public final org.slf4j.Logger getSlf4jLogger() {
        return this.getModContainer().getSlf4jLogger();
    }

    protected final void setConfigObject(@Nullable Object configObject) {
        this.getModContainer().setConfigObject(configObject);
    }

    @Nullable public final Object getConfigObject() {
        return this.getModContainer().getConfigObject();
    }

    public final void saveConfigObject() {
        ConfigIO.writeConfiguration(this.getModContainer(), this.getConfigObject());
    }

    public void onPreInit() {}

    public void onLatePreInit() {}

    public void onInit() {}

    public void onPostInit() {}

    public void onReceiveDataFromClient(@NotNull NetworkManager connection, byte[] data) throws IOException {}

    public void onReceiveDataFromServer(@NotNull NetworkManager connection, byte[] data) throws IOException {}

    public void onEventError(@NotNull Event event,@NotNull EventCallback callback,@NotNull Throwable throwable, boolean disable) {}
}
