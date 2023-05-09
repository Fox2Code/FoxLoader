package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.*;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class Mod implements LifecycleListener {
    ModContainer modContainer;

    public final ModContainer getModContainer() {
        return Objects.requireNonNull(modContainer == null ? ModContainer.tmp : modContainer);
    }

    /**
     * @param configObject to use for the config screen.
     */
    protected final void setConfigObject(Object configObject) {
        this.getModContainer().configObject = configObject;
    }

    /**
     * @return Object to use for the config screen.
     */
    public final Object getConfigObject() {
        return this.getModContainer().configObject;
    }

    /**
     * @return Object to use for the config screen.
     */
    public final Object getConfigFolder() {
        File configFolder = this.getModContainer().configFolder;
        if (!configFolder.isDirectory() && !configFolder.mkdirs()) {
            throw new IOError(new IOException("Can't create " + configFolder.getPath()));
        }
        return configFolder;
    }

    /**
     * @return mod logger
     */
    public final Logger getLogger() {
        return this.getModContainer().logger;
    }

    /**
     * Called when registry is editable and game didn't start yet, useful to register item/blocks.
     */
    public void onPreInit() {}

    /**
     * Called when registry is editable and game didn't start yet, useful to register item variants
     */
    public void onInit() {}

    /**
     *  Called when registry is frozen and game didn't start yet, useful to register recipes.
     */
    public void onPostInit() {}

    /**
     * Called when registry is editable and game didn't start yet, useful to register item variants
     */
    public void onTick() {}

    /**
     * Called when camera and render updated! (Only called client side)
     */
    public void onCameraAndRenderUpdated(float partialTick) {}

    /**
     * When server receive client packet.
     */
    public void onReceiveClientPacket(NetworkPlayer networkPlayer, byte[] data) {}

    /**
     * When client receive server packet.
     */
    public void onReceiveServerPacket(NetworkPlayer networkPlayer, byte[] data) {}

    /**
     * Send network data, has no effect in single player or
     * when remote server is not a FoxLoader supported server.
     *
     * @see NetworkPlayer#sendNetworkData(ModContainer, byte[])
     */
    public void sendNetworkData(NetworkPlayer networkPlayer, byte[] data) {
        networkPlayer.sendNetworkData(this.modContainer, data);
    }

    /**
     * Called when a remote player is joining the server, useful for sending mod config, etc...
     * <p>
     * Please note that it is not called in single player.
     */
    public void onNetworkPlayerJoined(NetworkPlayer networkPlayer) {}

    /**
     * Called when the server received client mod data
     */
    public void onNetworkPlayerHello(NetworkPlayer networkPlayer, ClientHello clientHello) {}

    /**
     * Called when a server/single-player/local-world start
     */
    public void onServerStart(NetworkPlayer.ConnectionType connectionType) {}

    /**
     * Called when a server/single-player/local-world stop
     */
    public void onServerStop(NetworkPlayer.ConnectionType connectionType) {}

    /**
     * Called when a player break a block
     * @return true to cancel the interaction.
     */
    public boolean onPlayerStartBreakBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                      int x, int y, int z, int facing, boolean cancelled) {
        return false;
    }

    /**
     * Called when a player break a block
     * @return true to cancel the interaction.
     */
    public boolean onPlayerBreakBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                      int x, int y, int z, int facing, boolean cancelled) {
        return false;
    }

    /**
     * Called when a player use an item
     * @return true to cancel the interaction.
     */
    public boolean onPlayerUseItem(NetworkPlayer networkPlayer, RegisteredItemStack itemStack, boolean cancelled) {
        return false;
    }

    /**
     * Called when a player use an item while pointing at a block
     * @return true to cancel the interaction.
     */
    public boolean onPlayerUseItemOnBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                          int x, int y, int z, int facing,
                                          float xOffset, float yOffset, float zOffset, boolean cancelled) {
        return false;
    }

    // GameRegistry mirror

    /**
     * @see GameRegistry#getRegisteredItem(int)
     */
    public RegisteredItem getRegisteredItem(int id) {
        return GameRegistry.getInstance().getRegisteredItem(id);
    }

    /**
     * @see GameRegistry#getRegisteredItem(String)
     */
    public RegisteredItem getRegisteredItem(String name) {
        if (name.indexOf(':') == -1) name = getModContainer().id + ":" + name;
        return GameRegistry.getInstance().getRegisteredItem(name);
    }

    /**
     * @see GameRegistry#getRegisteredBlock(int)
     */
    public RegisteredBlock getRegisteredBlock(int id) {
        return GameRegistry.getInstance().getRegisteredBlock(id);
    }

    /**
     * @see GameRegistry#getRegisteredBlock(String)
     */
    public RegisteredBlock getRegisteredBlock(String name) {
        if (name.indexOf(':') == -1) name = getModContainer().id + ":" + name;
        return GameRegistry.getInstance().getRegisteredBlock(name);
    }

    /**
     * @see GameRegistry#registerNewItem(String, ItemBuilder)
     */
    public RegisteredItem registerNewItem(String name, ItemBuilder itemBuilder) {
        return GameRegistry.getInstance().registerNewItem(getModContainer().id + ":" + name, itemBuilder);
    }

    /**
     * @see GameRegistry#registerNewBlock(String, BlockBuilder)
     */
    public RegisteredBlock registerNewBlock(String name, BlockBuilder blockBuilder) {
        return GameRegistry.getInstance().registerNewBlock(getModContainer().id + ":" + name, blockBuilder);
    }

    /**
     * @see GameRegistry#registerRecipe(RegisteredItemStack, Object...)
     */
    public void registerRecipe(RegisteredItemStack result, Object... recipe) {
         GameRegistry.getInstance().registerRecipe(result, recipe);
    }

    /**
     * @see GameRegistry#registerShapelessRecipe(RegisteredItemStack, GameRegistry.Ingredient...)
     */
    public void registerShapelessRecipe(RegisteredItemStack result, GameRegistry.Ingredient... ingredients) {
        GameRegistry.getInstance().registerShapelessRecipe(result, ingredients);
    }

    // For internal use only
    void loaderHandleServerHello(NetworkPlayer networkPlayer, ServerHello serverHello) {}
    void loaderHandleClientHello(NetworkPlayer networkPlayer, ClientHello clientHello) {}
    void loaderHandleDoFoxLoaderUpdate(String version, String url) throws IOException {
        System.err.println("Unhandled loaderHandleDoFoxLoaderUpdate()");
    }

    interface SidedMod {
        ModContainer getModContainer();
    }
}
