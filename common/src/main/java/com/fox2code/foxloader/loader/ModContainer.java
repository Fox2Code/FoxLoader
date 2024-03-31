package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.config.ConfigStructure;
import com.fox2code.foxloader.config.NoConfigObject;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.utils.FastThreadLocal;
import com.fox2code.foxloader.loader.lua.LuaVMHelper;
import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.loader.transformer.PreClassTransformer;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredEntity;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import com.google.gson.JsonObject;
import org.semver4j.Semver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ModContainer {
    private static final FastThreadLocal<ModContainer> activeModContainer = new FastThreadLocal<>();
    // tmp is used to make getModContainer work in constructor.
    static ModContainer tmp;
    public final File file;
    public final File configFolder;
    public final String id;
    public final String name;
    public final String version;
    public final Semver semver;
    public final String description;
    public final String jitpack;
    public final Logger logger;
    public final boolean unofficial;
    private final boolean injected;
    private Object configObject;
    String prePatch;
    Mod commonMod;
    String commonModCls;
    String commonMixins;
    Mod serverMod;
    String serverModCls;
    String serverMixins;
    Mod clientMod;
    String clientModCls;
    String clientMixins;

    ModContainer(File file, String id, String name, String version,
                 String description, String jitpack, boolean unofficial) {
        this(file, id, name, version, Semver.coerce(version), description, jitpack, unofficial, false);
    }

    ModContainer(File file, String id, String name, String version,
                 Semver semver, String description, String jitpack, boolean unofficial, boolean injected) {
        this.file = Objects.requireNonNull(file);
        this.configFolder = new File(ModLoader.config, id);
        this.id = id;
        this.name = name;
        this.version = version;
        this.semver = semver;
        this.description = description;
        this.jitpack = jitpack;
        this.logger = Logger.getLogger(name);
        FoxLauncher.installLoggerHelperOn(this.logger);
        this.unofficial = unofficial;
        this.injected = injected;
        if (ModLoader.DEV_MODE) {
            this.logger.setLevel(injected ? Level.ALL : Level.FINE);
        }
    }

    private ModContainer markActive() {
        ModContainer modContainer = activeModContainer.get();
        activeModContainer.set(this);
        return modContainer;
    }

    public static ModContainer getActiveModContainer() {
        return activeModContainer.get();
    }

    static void setActiveModContainer(ModContainer modContainer)  {
        if (modContainer == null) activeModContainer.remove();
        else activeModContainer.set(modContainer);
    }

    public void runInContext(Runnable runnable) {
        ModContainer modContainer = markActive();
        try {
            runnable.run();
        } finally {
            setActiveModContainer(modContainer);
        }
    }

    public <T, R> R runFuncInContext(T thing, Function<T, R> func) {
        ModContainer modContainer = markActive();
        R result;
        try {
            result = func.apply(thing);
        } finally {
            setActiveModContainer(modContainer);
        }
        return result;
    }

    public Mod getClientMod() {
        return clientMod;
    }

    public Mod getServerMod() {
        return serverMod;
    }

    public Mod getCommonMod() {
        return commonMod;
    }

    public Mod getMod() {
        Mod mod = FoxLauncher.isClient() ?
                clientMod : serverMod;
        return mod != null ? mod : commonMod;
    }

    public String getFileName() {
        return this.file == null ? "built-in" : this.file.getName();
    }

    void setConfigObject(Object configObject) {
        this.configObject = configObject;
        if (configObject != null && !(configObject instanceof NoConfigObject)) {
            ConfigStructure configStructure = ConfigStructure.parseFromClass(configObject.getClass(), this);
            File configFileDestination = new File(ModLoader.config, this.id + ".json");
            if (configFileDestination.exists()) {
                try {
                    configStructure.loadJsonConfig(ModLoader.gson.fromJson(new InputStreamReader(
                            Files.newInputStream(configFileDestination.toPath()),
                            StandardCharsets.UTF_8), JsonObject.class), configObject);
                } catch (Throwable t) {
                    ModLoader.getModLoaderLogger().log(Level.WARNING,
                            "Failed to read config file of " + this.id, t);
                }
            } else {
                try {
                    ModLoader.gson.toJson(configStructure.saveJsonConfig(configObject),
                            new OutputStreamWriter(Files.newOutputStream(configFileDestination.toPath()),
                                    StandardCharsets.UTF_8));
                } catch (Throwable t) {
                    ModLoader.getModLoaderLogger().log(Level.WARNING,
                            "Failed to save default config file of " + this.id, t);
                }
            }
        }
    }

    public Object getConfigObject() {
        return this.configObject;
    }

    public void saveModConfig() {
        if (this.configObject == null || this.configObject instanceof NoConfigObject) return;
        ConfigStructure configStructure = ConfigStructure.parseFromClass(this.configObject.getClass(), this);
        File configFileDestination = new File(ModLoader.config, this.id + ".json");
        try {
            ModLoader.gson.toJson(configStructure.saveJsonConfig(this.configObject),
                    new OutputStreamWriter(Files.newOutputStream(configFileDestination.toPath()),
                            StandardCharsets.UTF_8));
        } catch (Throwable t) {
            ModLoader.getModLoaderLogger().log(Level.WARNING,
                    "Failed to save config file of " + this.id, t);
        }
    }

    void applyPrePatch() throws ReflectiveOperationException {
        if (prePatch != null) {
            PreClassTransformer preClassTransformer =
                    Class.forName(prePatch, false, FoxLauncher.getFoxClassLoader())
                            .asSubclass(PreClassTransformer.class).newInstance();
            // Allow PrePatch to also be a mod.
            if (preClassTransformer instanceof Mod) {
                ((Mod) preClassTransformer).modContainer = this;
                if (commonModCls == null) {
                    commonMod = ((Mod) preClassTransformer);
                }
            }
            PreLoader.registerPrePatch(preClassTransformer);
        }
    }

    void applyModMixins(boolean client) {
        if (client) {
            if (ModLoaderMixin.addMixinConfigurationSafe(id, clientMixins)) {
                ModLoaderMixin.addMixinConfigurationSafe(id,
                        id + ".client.mixins.json", false);
            }
        } else {
            if (ModLoaderMixin.addMixinConfigurationSafe(id, serverMixins)) {
                ModLoaderMixin.addMixinConfigurationSafe(id,
                        id + ".server.mixins.json", false);
            }
        }
        ModLoaderMixin.addMixinConfigurationSafe(id, commonMixins);
    }

    void applyMod(boolean client) throws ReflectiveOperationException {
        if (!this.id.equals(ModLoader.FOX_LOADER_MOD_ID)) {
            if (client) {
                this.clientMod = initializeMod(this.clientModCls);
            } else {
                this.serverMod = initializeMod(this.serverModCls);
            }
            this.commonMod = initializeMod(this.commonModCls);
        }
        try (InputStream inputStream = ModContainer.class.getResourceAsStream("/assets/" + id + "/lang/en_US.lang")) {
            if (inputStream != null) {
                logger.log(Level.FINE, "Loaded /assets/" + id + "/lang/en_US.lang");
                ModLoader.Internal.fallbackTranslations.load(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                if (injected) {
                    Properties properties = new Properties();
                    loadLanguageTo("en_US", properties);
                    properties.forEach((o, o2) -> logger.log(Level.CONFIG, o + " -> " + o2));
                }
            } else if (injected) {
                logger.log(Level.WARNING, "Your mod don't have a /assets/" + id + "/lang/en_US.lang");
            }
        } catch (Throwable ignored) {}
        if (file.getName().endsWith(".lua") && this.commonMod == null) {
            try {
                this.commonMod = LuaVMHelper.loadMod(this);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to load " + file.getName(), e);
            }
        }
    }

    void loadLanguageTo(String lang, Properties target) {
        try (InputStream inputStream = ModContainer.class.getResourceAsStream(
                "/assets/" + id + "/lang/" + lang + ".lang")) {
            if (inputStream != null) {
                target.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {}
    }

    private Mod initializeMod(String clsName) throws ReflectiveOperationException {
        if (clsName == null) return null;
        Mod mod;
        Class<? extends Mod> cls = Class.forName(clsName, false,
                FoxLauncher.getFoxClassLoader()).asSubclass(Mod.class);
        try {
            tmp = this;
            mod = cls.newInstance();
        } finally {
            tmp = null;
        }
        LifecycleListener.register(mod);
        mod.modContainer = this;
        return mod;
    }

    public void notifyReceiveClientPacket(NetworkPlayer networkPlayer, byte[] data) {
        if (commonMod != null)
            commonMod.onReceiveClientPacket(networkPlayer, data);
        if (clientMod != null)
            clientMod.onReceiveClientPacket(networkPlayer, data);
        if (serverMod != null)
            serverMod.onReceiveClientPacket(networkPlayer, data);
    }

    public void notifyReceiveServerPacket(NetworkPlayer networkPlayer, byte[] data) {
        if (commonMod != null)
            commonMod.onReceiveServerPacket(networkPlayer, data);
        if (clientMod != null)
            clientMod.onReceiveServerPacket(networkPlayer, data);
        if (serverMod != null)
            serverMod.onReceiveServerPacket(networkPlayer, data);
    }

    void notifyNetworkPlayerJoined(NetworkPlayer networkPlayer) {
        if (clientMod != null)
            clientMod.onNetworkPlayerJoined(networkPlayer);
        if (serverMod != null)
            serverMod.onNetworkPlayerJoined(networkPlayer);
        if (commonMod != null)
            commonMod.onNetworkPlayerJoined(networkPlayer);
    }

    void notifyNetworkPlayerHello(NetworkPlayer networkPlayer, ClientHello clientHello) {
        if (clientMod != null)
            clientMod.onNetworkPlayerHello(networkPlayer, clientHello);
        if (serverMod != null)
            serverMod.onNetworkPlayerHello(networkPlayer, clientHello);
        if (commonMod != null)
            commonMod.onNetworkPlayerHello(networkPlayer, clientHello);
    }

    void notifyOnPreInit() {
        if (commonMod != null)
            commonMod.onPreInit();
        if (clientMod != null)
            clientMod.onPreInit();
        if (serverMod != null)
            serverMod.onPreInit();
    }

    void notifyOnInit() {
        if (commonMod != null)
            commonMod.onInit();
        if (clientMod != null)
            clientMod.onInit();
        if (serverMod != null)
            serverMod.onInit();
    }

    void notifyOnPostInit() {
        if (commonMod != null)
            commonMod.onPostInit();
        if (clientMod != null)
            clientMod.onPostInit();
        if (serverMod != null)
            serverMod.onPostInit();
    }

    void notifyOnTick() {
        if (commonMod != null)
            commonMod.onTick();
        if (clientMod != null)
            clientMod.onTick();
        if (serverMod != null)
            serverMod.onTick();
    }

    void notifyCameraAndRenderUpdated(float partialTick) {
        if (commonMod != null)
            commonMod.onCameraAndRenderUpdated(partialTick);
        if (clientMod != null)
            clientMod.onCameraAndRenderUpdated(partialTick);
        if (serverMod != null)
            serverMod.onCameraAndRenderUpdated(partialTick);
    }

    boolean notifyPlayerStartBreakBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                   int x, int y, int z, int facing, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onPlayerStartBreakBlock(networkPlayer, itemStack, x, y, z, facing, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onPlayerStartBreakBlock(networkPlayer, itemStack, x, y, z, facing, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onPlayerStartBreakBlock(networkPlayer, itemStack, x, y, z, facing, cancelled);
        return cancelled;
    }

    boolean notifyPlayerBreakBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                   int x, int y, int z, int facing, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onPlayerBreakBlock(networkPlayer, itemStack, x, y, z, facing, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onPlayerBreakBlock(networkPlayer, itemStack, x, y, z, facing, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onPlayerBreakBlock(networkPlayer, itemStack, x, y, z, facing, cancelled);
        return cancelled;
    }

    boolean notifyPlayerUseItem(NetworkPlayer networkPlayer, RegisteredItemStack itemStack, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onPlayerUseItem(networkPlayer, itemStack, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onPlayerUseItem(networkPlayer, itemStack, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onPlayerUseItem(networkPlayer, itemStack, cancelled);
        return cancelled;
    }

    boolean notifyPlayerUseItemOnBlock(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                       int x, int y, int z, int facing,
                                       float xOffset, float yOffset, float zOffset, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onPlayerUseItemOnBlock(networkPlayer, itemStack,
                    x, y, z, facing, xOffset, yOffset, zOffset, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onPlayerUseItemOnBlock(networkPlayer, itemStack,
                    x, y, z, facing, xOffset, yOffset, zOffset, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onPlayerUseItemOnBlock(networkPlayer, itemStack,
                    x, y, z, facing, xOffset, yOffset, zOffset, cancelled);
        return cancelled;
    }

    boolean notifyPlayerUseItemOnEntity(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                        RegisteredEntity targetEntity, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onPlayerUseItemOnEntity(
                    networkPlayer, itemStack, targetEntity, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onPlayerUseItemOnEntity(
                    networkPlayer, itemStack, targetEntity, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onPlayerUseItemOnEntity(
                    networkPlayer, itemStack, targetEntity, cancelled);
        return cancelled;
    }

    boolean notifyPlayerAttackEntity(NetworkPlayer networkPlayer, RegisteredItemStack itemStack,
                                     RegisteredEntity targetEntity, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onPlayerAttackEntity(
                    networkPlayer, itemStack, targetEntity, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onPlayerAttackEntity(
                    networkPlayer, itemStack, targetEntity, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onPlayerAttackEntity(
                    networkPlayer, itemStack, targetEntity, cancelled);
        return cancelled;
    }

    boolean notifyNetworkPlayerDisconnected(NetworkPlayer networkPlayer, String kickMessage, boolean cancelled) {
        if (commonMod != null)
            cancelled |= commonMod.onNetworkPlayerDisconnected(
                    networkPlayer, kickMessage, cancelled);
        if (clientMod != null)
            cancelled |= clientMod.onNetworkPlayerDisconnected(
                    networkPlayer, kickMessage, cancelled);
        if (serverMod != null)
            cancelled |= serverMod.onNetworkPlayerDisconnected(
                    networkPlayer, kickMessage, cancelled);
        return cancelled;
    }
}
