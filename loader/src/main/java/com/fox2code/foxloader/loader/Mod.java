package com.fox2code.foxloader.loader;

import com.fox2code.foxevents.Event;
import com.fox2code.foxevents.EventCallback;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.networking.NetworkManager;
import net.minecraft.common.recipe.CraftingManager;
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

    public void onPreInit() {}

    public void onInit() {}

    public void onPostInit() {}

    public void onReceiveDataFromClient(NetworkManager connection, byte[] data) throws IOException {}

    public void onReceiveDataFromServer(NetworkManager connection, byte[] data) throws IOException {}

    public void onEventError(Event event, EventCallback callback, Throwable throwable, boolean disable) {}
}
