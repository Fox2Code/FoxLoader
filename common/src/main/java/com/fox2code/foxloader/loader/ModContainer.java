package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.loader.transformer.PreClassTransformer;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.BlockBuilder;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.GameRegistry;
import com.fox2code.foxloader.registry.RegisteredBlock;
import org.semver4j.Semver;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ModContainer {
    // tmp is used to make getModContainer work in constructor.
    static ModContainer tmp;
    public final File file;
    public final String id;
    public final String name;
    public final String version;
    public final Semver semver;
    public final String description;
    public final String jitpack;
    public final Logger logger;
    private final boolean injected;
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
    Object configObject;

    ModContainer(File file, String id, String name, String version, String description, String jitpack) {
        this(file, id, name, version, Semver.coerce(version), description, jitpack, false);
    }

    ModContainer(File file, String id, String name, String version,
                 Semver semver, String description, String jitpack, boolean injected) {
        this.file = Objects.requireNonNull(file);
        this.id = id;
        this.name = name;
        this.version = version;
        this.semver = semver;
        this.description = description;
        this.jitpack = jitpack;
        this.logger = Logger.getLogger(name);
        this.injected = injected;
        if (ModLoader.DEV_MODE) {
            this.logger.setLevel(injected ? Level.ALL : Level.FINE);
        }
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

    public Object getConfigObject() {
        return configObject;
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
            if (!ModLoaderMixin.addMixinConfigurationSafe(id, clientMixins)) {
                ModLoaderMixin.addMixinConfigurationSafe(id,
                        id + ".client.mixins.json", false);
            }
        } else {
            if (!ModLoaderMixin.addMixinConfigurationSafe(id, serverMixins)) {
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
}
