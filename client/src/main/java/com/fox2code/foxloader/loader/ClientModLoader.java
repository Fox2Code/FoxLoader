package com.fox2code.foxloader.loader;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.launcher.utils.NetUtils;
import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.launcher.utils.SourceUtil;
import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistryClient;
import com.fox2code.foxloader.updater.UpdateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.mitask.PlayerCommandHandler;
import net.minecraft.src.client.gui.StringTranslate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;

public final class ClientModLoader extends ModLoader {
    public static final boolean linuxFix = Boolean.parseBoolean(
            System.getProperty("foxloader.linux-fix", // Switch to enable linux workaround
                    Boolean.toString(Platform.getPlatform() == Platform.LINUX)));
    public static boolean showFrameTimes;
    private static byte[] clientHello;

    public static void launchModdedClient(String... args) {
        ModLoader.foxLoader.clientMod = new ClientModLoader();
        ModLoader.foxLoader.clientMod.modContainer = ModLoader.foxLoader;
        Objects.requireNonNull(ModLoader.foxLoader.getMod(), "WTF???");
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
                    new ArrayList<>(ModLoader.modContainers.size() +
                            ModLoader.coreMods.size());
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
    public void onReceiveServerPacket(NetworkPlayer networkPlayer, byte[] data) {
        ModLoader.foxLoader.logger.info("Received server packet");
        LoaderNetworkManager.executeServerPacketData(networkPlayer, data);
    }

    @Override
    void loaderHandleServerHello(NetworkPlayer networkPlayer, ServerHello serverHello) {
        ModLoader.foxLoader.logger.info("Initializing id translator");
        GameRegistryClient.initializeMappings(serverHello);
        ModLoader.foxLoader.logger.info("Ids translated!");
        networkPlayer.sendNetworkData(ModLoader.foxLoader, clientHello);
    }

    @Override
    void loaderHandleDoFoxLoaderUpdate(String version, String url) throws IOException {
        File dest = null;
        String[] args;
        LauncherType launcherType = FoxLauncher.getLauncherType();
        getLogger().info("Updating to " + version + " from " + launcherType + " launcher");
        switch (launcherType) {
            default:
                return;
            case BETA_CRAFT:
                File betacraftSource = SourceUtil.getSourceFile(ClientModLoader.class).getParentFile();
                String endPath = ".betacraft/versions";
                String path = betacraftSource.getPath();
                if (!path.replace('\\', '/').endsWith(endPath)) {
                    this.getLogger().warning("Not BetaCraft?");
                    return;
                }
                args = new String[]{null, "-jar", null, "--update", launcherType.name(),
                        path.substring(0, path.length() - endPath.length())};
                break;
            case MMC_LIKE:
                File libraries = ModLoader.foxLoader.file.getParentFile();
                dest = new File(libraries, "foxloader-" + version + ".jar");
            case VANILLA_LIKE:
                args = new String[]{null, "-jar", null, "--update", launcherType.name()};
        }
        if (dest == null) {
            if (!ModLoader.updateTmp.exists() && !ModLoader.updateTmp.mkdirs()) {
                this.getLogger().warning("Unable to create update tmp folder.");
                return;
            }
            dest = new File(ModLoader.updateTmp, "foxloader-" + version + ".jar");
        }
        if (BuildConfig.FOXLOADER_VERSION.equals(version) &&
                FoxLauncher.getLauncherType() != LauncherType.BIN) {
            // Can happen if wrongly installed
            if (!dest.equals(FoxLauncher.foxLoaderFile)) {
                Files.copy(FoxLauncher.foxLoaderFile.toPath(), dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
                NetUtils.downloadTo(url, fileOutputStream);
            }
        }
        args[0] = Platform.getPlatform().javaBin.getPath();
        args[2] = dest.getAbsolutePath();
        getLogger().info("Command: " + Arrays.toString(args));
        final Process process = new ProcessBuilder(args).directory(dest.getParentFile()).start();
        if (process.isAlive()) {
            new Thread(() -> {
                Scanner scanner = new Scanner(process.getInputStream());
                String line;
                while (process.isAlive() &&
                        (line = scanner.next()) != null) {
                    System.out.println("Update: " + line);
                    Thread.yield();
                }
                if (!process.isAlive()) {
                    System.out.println("Updated with exit code " + process.exitValue());
                }
            }, "Output log thread");
        } else {
            System.out.println("Updated with exit code " + process.exitValue());
        }
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
            UpdateManager.getInstance().initialize();
            GameRegistryClient.freeze();
            ModLoader.postInitializeMods();
            StringTranslate.reloadKeys();
            GameRegistryClient.freezeRecipes();
            // StatList.initBreakableStats();
            // StatList.initStats();
            PlayerCommandHandler.instance.reloadCommands();
            UpdateManager.getInstance().checkUpdates();
        }

        public static void notifyCameraAndRenderUpdated(float partialTick) {
            for (ModContainer modContainer : ModLoader.modContainers.values()) {
                modContainer.notifyCameraAndRenderUpdated(partialTick);
            }
        }
    }
}
