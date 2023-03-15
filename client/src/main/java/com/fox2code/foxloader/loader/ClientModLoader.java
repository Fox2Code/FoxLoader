package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.utils.NetUtils;
import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.mitask.PlayerCommandHandler;
import net.minecraft.src.client.gui.StringTranslate;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;

public final class ClientModLoader extends Mod {
    public static final boolean linuxFix = Boolean.parseBoolean(
            System.getProperty("foxloader.linux-fix", // Switch to enable linux workaround
                    Boolean.toString(Platform.getPlatform() == Platform.LINUX)));
    public static boolean showFrameTimes;
    private static byte[] clientHello;

    public static void launchModdedClient(String... args) {
        ModLoader.foxLoader.clientMod = new ClientModLoader();
        ModLoader.foxLoader.clientMod.modContainer = ModLoader.foxLoader;
        ModLoader.initializeModdedInstance(true);
        Platform.getPlatform().setupLwjgl2();
        ClientSelfTest.selfTest();
        computeClientHello();
        Minecraft.main(args);
    }

    private static void computeClientHello() {
        final byte[] nullSHA256 = new byte[32];
        try {
            ArrayList<ClientHello.ClientModData> clientModData =
                    new ArrayList<>(ModLoader.modContainers.size());
            for (File coreMod : ModLoader.coreMods) {
                byte[] sha256 = NetUtils.hashOf(coreMod);
                clientModData.add(new ClientHello.ClientModData(
                        coreMod.getName(), sha256, "", ""));
            }
            for (ModContainer modContainer : ModLoader.modContainers.values()) {
                byte[] sha256 = nullSHA256;
                if (modContainer.file != null) {
                    sha256 = NetUtils.hashOf(modContainer.file);
                }
                clientModData.add(new ClientHello.ClientModData(
                        modContainer.id, sha256,
                        modContainer.name, modContainer.version));
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(0); // <- Packet ID
            new ClientHello(clientModData).writeData(new DataOutputStream(byteArrayOutputStream));
            clientHello = byteArrayOutputStream.toByteArray();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private ClientModLoader() {}

    @Override
    public void onServerStart(NetworkPlayer.ConnectionType connectionType) {
        GameRegistryClient.resetMappings(connectionType.isServer);
    }

    @Override
    void loaderHandleServerHello(NetworkPlayer networkPlayer, ServerHello serverHello) {
        ModLoader.foxLoader.logger.info("Initializing id translator");
        GameRegistryClient.initializeMappings(serverHello);
        ModLoader.foxLoader.logger.info("Ids translated!");
        networkPlayer.sendNetworkData(ModLoader.foxLoader, clientHello);
    }

    public static class Internal {
        public static byte[] networkChunkBytes = null;
        private static final HashMap<String, Properties> translationsCache = new HashMap<>();
        private static final Function<String, Properties> translationsCacheFiller = lang -> {
            Properties properties = new Properties();
            for (ModContainer modContainer : ModLoader.modContainers.values()) {
                modContainer.loadLanguageTo(lang, properties);
            }
            return properties;
        };

        static {
            translationsCache.put("en_US", ModLoader.Internal.fallbackTranslations);
        }

        public static Properties getTranslationsForLanguage(String lang) {
            if (!ModLoader.areAllModsLoaded()) return ModLoader.Internal.fallbackTranslations;
            return translationsCache.computeIfAbsent(lang, translationsCacheFiller);
        }

        public static void notifyRun() {
            GameRegistryClient.initialize();
            ModLoader.initializeMods(true);
            GameRegistryClient.freeze();
            ModLoader.postInitializeMods();
            StringTranslate.reloadKeys();
            GameRegistryClient.freezeRecipes();
            // StatList.initBreakableStats();
            // StatList.initStats();
            PlayerCommandHandler.instance.reloadCommands();
        }

        public static void notifyCameraAndRenderUpdated(float partialTick) {
            for (ModContainer modContainer : ModLoader.modContainers.values()) {
                modContainer.notifyCameraAndRenderUpdated(partialTick);
            }
        }
    }
}
