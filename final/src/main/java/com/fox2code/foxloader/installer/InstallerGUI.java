package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.DependencyHelper;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.launcher.StackTraceStringifier;
import com.fox2code.foxloader.launcher.utils.IOUtils;
import com.fox2code.foxloader.launcher.utils.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class InstallerGUI implements FileDropHelper.FileDropHandler {
    public static final String DEFAULT_VERSION_NAME =
            "ReIndev-" + BuildConfig.REINDEV_VERSION +
                    "-FoxLoader-" + BuildConfig.FOXLOADER_VERSION;
    private static final String CUSTOM_VERSION_NAME_BASE =
            "FoxLoader-" + BuildConfig.FOXLOADER_VERSION;
    private static final String DEFAULT_TITLE =
            "FoxLoader " + BuildConfig.FOXLOADER_VERSION + " - Installer";
    private static final String CUSTOM_TITLE =
            "FoxLoader* " + BuildConfig.FOXLOADER_VERSION + " - Installer";
    private static final String DEFAULT_LABEL =
            "For ReIndev " + BuildConfig.REINDEV_VERSION;
    private static final String CUSTOM_LABEL =
            "For ReIndev " + BuildConfig.REINDEV_VERSION + "?";
    private static final String USER_INSTRUCTION = // I thought I would never need to add that.
            " \n(You can select the FoxLoader version you want to use in the version list)";
    private static final String FULLSCREEN_LABEL =
            "FoxLoader " + BuildConfig.FOXLOADER_VERSION + " for ReIndev " + BuildConfig.REINDEV_VERSION;
    private static final int PROGRESS_BAR_MAX = DependencyHelper.commonDependencies.length + 1;
    private static final HashSet<String> MMC_PATCHES = new HashSet<>(Arrays.asList(
            "com.fox2code.foxloader.json", "net.minecraft.json", "net.minecraftforge.json"));
    private final InstallerPlatform installerPlatform;
    private final LauncherType launcherType;
    private final JFrame jFrame;
    private final Dimension minDimensions;
    private final JPanel globalContainer;
    private final JLabel label;
    private final FileDropHelper dropHelper;
    private final JButton minecraftButton;
    private final JButton mmcButton;
    private final JButton serverButton;
    private final JProgressBar progressBar;
    private boolean runningTask;
    private File reIndevSource;
    private String versionName;

    public InstallerGUI(InstallerPlatform installerPlatform) {
        this.installerPlatform = installerPlatform;
        this.launcherType = null;
        versionName = DEFAULT_VERSION_NAME;
        jFrame = new JFrame(DEFAULT_TITLE);
        jFrame.setMinimumSize(minDimensions = new Dimension(260, 30));
        TranslateEngine.updateOnTranslate(jFrame);
        if (installerPlatform.fullscreen) {
            jFrame.setUndecorated(true);
        }
        globalContainer = makeContainer(null);
        label = new JLabel(DEFAULT_LABEL);
        TranslateEngine.installOnFormat(label, "installer.for-reindev", BuildConfig.REINDEV_VERSION);
        dropHelper = new FileDropHelper(globalContainer, this);
        JPanel languageContainer = makeContainer("installer.language");
        languageContainer.add(TranslateEngine.makeLanguageSelectComponent());
        JPanel clientContainer = makeContainer("installer.install-client");
        JButton minecraftButton;
        JButton mmcButton;
        if (installerPlatform.specialLauncher) {
            minecraftButton = makeButton(clientContainer,
                    "installer.install-special", this::installMineCraft, installerPlatform.platformName);
            mmcButton = null;
        } else {
            minecraftButton = makeButton(clientContainer,
                    "installer.install-minecraft", this::installMineCraft);
            makeButton(clientContainer,
                    "installer.install-betacraft", this::installBetaCraft);
            mmcButton = makeButton(clientContainer,
                    "installer.extract-multimc", this::extractMMCInstance);
        }
        this.mmcButton = mmcButton;
        this.minecraftButton = minecraftButton;
        JButton serverButton = null;
        if (this.installerPlatform.fullscreenLayout) {
            makeButton(clientContainer, "installer.exit-installer", this::exitInstaller);
        } else {
            JPanel serverContainer = makeContainerEx("installer.install-server", true);
            serverButton = makeButton(serverContainer, "installer.extract-server", this::extractServer);
            serverContainer.add(new SelectableTranslatableLabel("installer.install-server.text.*"));
        }
        this.serverButton = serverButton;
        progressBar = new JProgressBar();
        progressBar.setMaximum(PROGRESS_BAR_MAX);
        progressBar.setString("");
        progressBar.setStringPainted(true);
        if (this.installerPlatform.fullscreenLayout) {
            globalContainer.add(progressBar);
        }

        jFrame.setLayout(new BorderLayout());
        jFrame.add(BorderLayout.NORTH, label);
        jFrame.add(BorderLayout.CENTER, globalContainer);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);
        if (!this.installerPlatform.fullscreenLayout) {
            jFrame.setResizable(false);
        }
        jFrame.pack();
    }

    public InstallerGUI(InstallerPlatform installerPlatform, LauncherType launcherType) {
        this.installerPlatform = installerPlatform;
        this.launcherType = launcherType;
        versionName = DEFAULT_VERSION_NAME;
        jFrame = null;
        minDimensions = null;
        globalContainer = null;
        label = null;
        dropHelper = null;
        minecraftButton = null;
        mmcButton = null;
        serverButton = null;
        progressBar = new JProgressBar();
        progressBar.setMaximum(PROGRESS_BAR_MAX);
    }

    private JPanel makeContainer(final String text) {
        return this.makeContainerEx(text, false);
    }

    private JPanel makeContainerEx(final String text, final boolean specialLayout) {
        final JPanel container = new JPanel();
        if (text != null) {
            container.setLayout(installerPlatform.fullscreenLayout ?
                    new FlowLayout(FlowLayout.CENTER) :
                    // specialLayout is a workaround for layout building.
                    specialLayout ? new VerticalGridBagLayout() :
                            new GridLayout(0, 1, 0, 3));
            container.setBorder(TranslateEngine.createTitledBorder(text));
            globalContainer.add(container);
        } else {
            container.setLayout(installerPlatform.fullscreenLayout ?
                    new VerticalGridBagLayout() : new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        }
        return container;
    }

    private JButton makeButton(final JPanel panel,final String text,final Runnable action,final String... extra) {
        final JButton button = new JButton(text);
        if (extra.length == 0) {
            TranslateEngine.installOn(button, text);
        } else {
            TranslateEngine.installOnFormat(button, text, extra);
        }
        button.setFocusPainted(false);
        button.setMinimumSize(minDimensions);
        button.setPreferredSize(minDimensions);
        button.setMargin(new Insets(0, 0, 0, 0));
        if (action == null) {
            button.setEnabled(false);
        } else {
            button.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (runningTask) return;
                    runningTask = true;
                    try {
                        action.run();
                    } finally {
                        runningTask = false;
                    }
                }
            });
        }
        panel.add(button);
        return button;
    }

    public void show() {
        if (this.installerPlatform.fullscreen) {
            this.jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.jFrame.setAlwaysOnTop(true);
        } else {
            if (!this.installerPlatform.specialLauncher) {
                this.dropHelper.enableDragAndDrop();
            }
            if (this.installerPlatform.fullscreenLayout) {
                this.jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        }
        this.jFrame.setVisible(true);
        if (this.installerPlatform.fullscreen) {
            GraphicsDevice graphicsDevice = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getScreenDevices()[0];
            if (graphicsDevice.isFullScreenSupported()) {
                graphicsDevice.setFullScreenWindow(this.jFrame);
            } else {
                this.jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        }
    }

    private boolean checkInstaller(boolean checkExtStrict) {
        if (this.reIndevSource != null &&
                !this.reIndevSource.exists()) {
            if (this.jFrame == null) return true;
            // This cannot happen in true fullscreen
            JOptionPane.showMessageDialog(this.jFrame,
                    "Provided custom jar got deleted?",
                    this.jFrame.getTitle(), JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return true;
        }
        if (Main.currentInstallerFile.isDirectory()) {
            showMessage(TranslateEngine.getTranslation("installer.error.run-from-ide"), true);
            return true;
        }
        if (checkExtStrict && !Main.currentInstallerFile.getName()
                .toLowerCase(Locale.ROOT).endsWith(".jar") &&
                !this.installerPlatform.specialLauncher) {
            if (this.jFrame == null) return true;
            JOptionPane.showMessageDialog(this.jFrame,
                    TranslateEngine.getTranslation("installer.error.invalid-extension"),
                    this.jFrame.getTitle(), JOptionPane.QUESTION_MESSAGE);
            return true;
        }
        return false;
    }

    public void installMineCraft() {
        if (this.checkInstaller(false)) {
            return;
        }
        if (this.installerPlatform.specialLauncher) {
            DependencyHelper.setMCLibraryRoot();
            for (int i = 0; i < DependencyHelper.commonDependencies.length; i++) {
                DependencyHelper.loadDependency(DependencyHelper.commonDependencies[i]);
                progressBar.setValue(i + 1);
            }
        }
        DependencyHelper.loadDependencySelf(DependencyHelper.GSON_DEPENDENCY);
        File minecraft = Platform.getAppDir("minecraft");
        File versions = new File(minecraft, "versions");
        File launcherProfiles = new File(minecraft, "launcher_profiles.json");
        File foxLoaderVersion = new File(versions, this.versionName);
        File foxLoaderVersionJar = new File(foxLoaderVersion, this.versionName + ".jar");
        File foxLoaderVersionJson = new File(foxLoaderVersion, this.versionName + ".json");
        if (!foxLoaderVersion.isDirectory() && !foxLoaderVersion.mkdirs()) {
            showMessage(TranslateEngine.getTranslationFormat(
                    "installer.error.create-target-directory", "version"), true);
            return;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                    "/launcher-version.json"), byteArrayOutputStream);
            IOUtils.copyAndClose(new ByteArrayInputStream(byteArrayOutputStream.toString()
                            .replace(installerPlatform.specialLauncher ?
                                    "#hack_releaseTime#" : "#no-op#", "releaseTime")
                            .replace("#version#", this.versionName).getBytes(StandardCharsets.UTF_8)),
                    Files.newOutputStream(foxLoaderVersionJson.toPath()));
            Files.copy(Main.currentInstallerFile.toPath(),
                    foxLoaderVersionJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ProfileInstaller.install(launcherProfiles);
        } catch (IOException e) {
            showError(e);
            return;
        }
        progressBar.setValue(PROGRESS_BAR_MAX);
        showMessage(TranslateEngine.getTranslationFormat(
                "installer.success", BuildConfig.FOXLOADER_VERSION, BuildConfig.REINDEV_VERSION) + "\n" +
                TranslateEngine.getTranslation("installer.comment"), false);
    }

    public void installBetaCraft() {
        File betacraft = Platform.getAppDir("betacraft");
        if (!betacraft.exists()) {
            File betacraftPortable = new File(".betacraft").getAbsoluteFile();
            if (betacraftPortable.exists()) {
                betacraft = betacraftPortable;
            }
        }
        this.installBetaCraftEx(betacraft);
    }

    public void installBetaCraftEx(File betaCraft) {
        if (this.checkInstaller(false)) {
            return;
        }
        File versions = new File(betaCraft, "versions");
        File versionsJsons = new File(versions, "jsons");
        File launchMethods = new File(betaCraft,
                "launcher" + File.separator + "launch-methods");
        File instances = new File(betaCraft,
                "launcher" + File.separator + "instances");
        if ((!versionsJsons.isDirectory() && !versionsJsons.mkdirs()) ||
                (!launchMethods.isDirectory() && !launchMethods.mkdirs()) ||
                (!instances.isDirectory() && !instances.mkdirs())) {
            showMessage(TranslateEngine.getTranslationFormat(
                    "installer.error.create-target-directory", "betacraft"), true);
            return;
        }
        File foxLoaderVersionJar = new File(versions, this.versionName + ".jar");
        File foxLoaderVersionJson = new File(versionsJsons, this.versionName + ".info");
        File foxLoaderVersionInstance = new File(instances, this.reIndevSource == null ?
                "ReIndev-" + BuildConfig.REINDEV_VERSION + "-FoxLoader.txt" : "ReIndev-custom-FoxLoader.txt");
        File foxLoaderBetaCraft = new File(launchMethods, "FoxLoader.jar");
        try {
            if (this.reIndevSource != null) {
                File reIndevSourceTarget = new File(versions, this.reIndevSource.getName());
                if (!reIndevSourceTarget.exists() ||
                        reIndevSourceTarget.length() != this.reIndevSource.length()) {
                    Files.copy(this.reIndevSource.toPath(), reIndevSourceTarget.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.copy(Main.currentInstallerFile.toPath(),
                    foxLoaderVersionJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            try (UnixPrintStream printStream = new UnixPrintStream(
                    Files.newOutputStream(foxLoaderVersionJson.toPath()))) {
                printStream.println("release-date:0");
                printStream.println("compile-date:0");
                printStream.println("launch-method:FoxLoader");
                printStream.println("proxy-args:-XX:+IgnoreUnrecognizedVMOptions");
                printStream.println("custom:true");
            }
            try (UnixPrintStream printStream = new UnixPrintStream(
                    Files.newOutputStream(foxLoaderVersionInstance.toPath()))) {
                printStream.println("name:FoxLoader " + BuildConfig.FOXLOADER_VERSION +
                        (this.reIndevSource == null ? "" : " (Custom jar)"));
                printStream.println("launchArgs:" + Main.optJvmArgsWithMem);
                printStream.println("width:854");
                printStream.println("height:480");
                printStream.println("proxy:false");
                printStream.println("keepopen:false");
                printStream.println("RPC:true");
                printStream.println("gameDir:" + Platform.getAppDir("reindev").getAbsolutePath());
                printStream.println("version:" + this.versionName);
                printStream.println("console:true");
                // On Linux it may default to an invalid JVM that crash the game.
                if (Platform.getPlatform() == Platform.LINUX &&
                        new File("/usr/lib/jvm/java-8-openjdk/bin/java").exists()) {
                    printStream.println("javaPath:/usr/lib/jvm/java-8-openjdk/bin/java");
                }
                printStream.println("addons:");
            }
            IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                            "/betacraft-" + BuildConfig.FOXLOADER_VERSION + ".jar"),
                    Files.newOutputStream(foxLoaderBetaCraft.toPath()));
        } catch (IOException e) {
            showError(e);
            return;
        }
        progressBar.setValue(PROGRESS_BAR_MAX);
        showMessage(TranslateEngine.getTranslationFormat(
                "installer.success", BuildConfig.FOXLOADER_VERSION, BuildConfig.REINDEV_VERSION) + "\n" +
                TranslateEngine.getTranslation("installer.comment"), false);
    }

    public void extractMMCInstance() {
        if (this.checkInstaller(true)) {
            return;
        }

        String fileName = Main.currentInstallerFile.getName();
        File instanceDest = new File(Main.currentInstallerFile.getParentFile(),
                fileName.substring(0, fileName.length() - 4) + "-mmc.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(instanceDest.toPath()))) {
            zipOutputStream.putNextEntry(new ZipEntry("libraries/foxloader-" + BuildConfig.FOXLOADER_VERSION + ".jar"));
            copyCloseIn(Files.newInputStream(Main.currentInstallerFile.toPath()), zipOutputStream);
            zipOutputStream.closeEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (String entry : new String[]{"patches/com.fox2code.foxloader.json",
                    "patches/net.minecraft.json", "patches/net.minecraftforge.json",
                    "instance.cfg", "mmc-pack.json"}) {
                zipOutputStream.putNextEntry(new ZipEntry(entry));
                byteArrayOutputStream.reset();
                IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                        "/mmc/" + entry), byteArrayOutputStream);
                copyCloseIn(new ByteArrayInputStream(byteArrayOutputStream.toString()
                        .replace("#version#", this.versionName)
                        .replace("#jvm_args#", Main.optJvmArgs)
                        .replace("#foxloader_version#", BuildConfig.FOXLOADER_VERSION)
                        .getBytes(StandardCharsets.UTF_8)), zipOutputStream);
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
        } catch (IOException e) {
            showError(e);
            return;
        }
        progressBar.setValue(PROGRESS_BAR_MAX);
        showMessage(TranslateEngine.getTranslationFormat("installer.multimc.text.*",
                BuildConfig.FOXLOADER_VERSION, BuildConfig.REINDEV_VERSION), false);
    }

    public void extractServer() {
        if (this.checkInstaller(true)) {
            return;
        }

        String fileName = Main.currentInstallerFile.getName();
        File serverDest = new File(Main.currentInstallerFile.getParentFile(),
                fileName.substring(0, fileName.length() - 4) + "-server.jar");
        try (ZipInputStream zipInputStream = new ZipInputStream(
                Files.newInputStream(Main.currentInstallerFile.toPath()));
             ZipOutputStream zipOutputStream = new ZipOutputStream(
                     Files.newOutputStream(serverDest.toPath()))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals("META-INF/MANIFEST.MF")) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(zipInputStream, byteArrayOutputStream);
                    zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                    copyCloseIn(new ByteArrayInputStream(byteArrayOutputStream.toString()
                            .replace("com.fox2code.foxloader.installer.Main",
                                    "com.fox2code.foxloader.launcher.ServerMain")
                            .getBytes(StandardCharsets.UTF_8)), zipOutputStream);
                    zipOutputStream.closeEntry();
                } else {
                    zipOutputStream.putNextEntry(zipEntry);
                    IOUtils.copy(zipInputStream, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        } catch (IOException e) {
            showError(e);
            return;
        }
        progressBar.setValue(PROGRESS_BAR_MAX);
        showMessage(TranslateEngine.getTranslationFormat("installer.server.text.*",
                BuildConfig.FOXLOADER_VERSION, BuildConfig.REINDEV_VERSION), false);
    }

    public void doSilentInstall(String arg) throws IOException {
        if (this.checkInstaller(false)) {
            return;
        }
        switch (this.launcherType) {
            default: {
                System.out.println("Unsupported launcherType: " + this.launcherType);
                return;
            }
            case BETA_CRAFT: {
                if (arg == null) {
                    this.installBetaCraft();
                } else {
                    this.installBetaCraftEx(new File(arg));
                }
                if (progressBar.getValue() != PROGRESS_BAR_MAX) {
                    System.exit(-1);
                    return;
                }
                break;
            }
            case VANILLA_LIKE: {
                this.installMineCraft();
                if (progressBar.getValue() != PROGRESS_BAR_MAX) {
                    System.exit(-1);
                    return;
                }
                break;
            }
            case MMC_LIKE: {
                File root = Main.currentInstallerFile.getParentFile().getParentFile();
                File patches = new File(root, "patches");
                File patch = new File(patches, "com.fox2code.foxloader.json");
                if (!patches.exists()) {
                    System.exit(-1);
                    return;
                }
                if (!patch.exists()) {
                    for (File file : Objects.requireNonNull(patches.listFiles())) {
                        if (file.getName().endsWith(".json") &&
                                !MMC_PATCHES.contains(file.getName())) {
                            if (!file.delete()) file.deleteOnExit();
                        }
                    }
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                        "/mmc/patches/com.fox2code.foxloader.json"), byteArrayOutputStream);
                IOUtils.copyAndClose(new ByteArrayInputStream(byteArrayOutputStream.toString()
                        .replace("#version#", this.versionName)
                        .replace("#foxloader_version#", BuildConfig.FOXLOADER_VERSION)
                        .getBytes(StandardCharsets.UTF_8)), Files.newOutputStream(patch.toPath()));
                for (String entry : new String[]{ // Fix in place replace!
                        "patches/net.minecraft.json", "patches/net.minecraftforge.json"}) {
                    IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                                    "/mmc/patches/com.fox2code.foxloader.json"),
                            Files.newOutputStream(new File(entry).toPath()));
                }
            }
        }
    }

    public void showError(Throwable throwable) {
        throwable.printStackTrace(System.out);
        if (this.jFrame == null) return;
        if (this.installerPlatform.fullscreen) {
            this.progressBar.setString(throwable.toString());
        } else {
            JOptionPane.showMessageDialog(this.jFrame.isVisible() ? this.jFrame : null,
                    StackTraceStringifier.stringifyStackTrace(throwable),
                    this.jFrame.getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showMessage(String message, boolean error) {
        System.out.println(message);
        if (this.jFrame == null) return;
        if (this.installerPlatform.fullscreen) {
            this.progressBar.setString(message);
        } else {
            JOptionPane.showMessageDialog(this.jFrame.isVisible() ? this.jFrame : null,
                    message, this.jFrame.getTitle(), error ?
                            JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void exitInstaller() {
        System.exit(0);
    }

    private static void copyCloseIn(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (InputStream is = inputStream) {
            byte[] byteChunk = new byte[4096];
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                outputStream.write(byteChunk, 0, n);
            }
        }
    }

    @Override
    public boolean areFilesAcceptable(List<File> fileList) {
        if (runningTask || fileList.size() != 1) return false;
        return fileList.get(0).getPath().endsWith(".jar");
    }

    @Override
    public boolean acceptFiles(List<File> fileList) {
        if (runningTask || fileList.size() != 1) return false;
        this.runningTask = true;
        try {
            File file = fileList.get(0);

            try (ZipFile zipFile = new ZipFile(file)) {
                if (zipFile.getEntry("net/minecraft/client/Minecraft.class") == null ||
                        zipFile.getEntry("net/minecraft/mitask/PlayerCommandHandler.class") == null) {
                    JOptionPane.showMessageDialog(this.jFrame,
                            TranslateEngine.getTranslation("installer.error.not-reindev-client"));
                    return false;
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this.jFrame,
                        TranslateEngine.getTranslation("installer.error.invalid-jar-file"));
                return false;
            }

            this.minecraftButton.setEnabled(false);
            this.mmcButton.setEnabled(false);
            if (this.serverButton != null)
                this.serverButton.setEnabled(false);
            this.versionName = CUSTOM_VERSION_NAME_BASE + "-with-" +
                    file.getName().substring(0, file.getName().length() - 4);
            this.reIndevSource = file;
            this.label.setText(CUSTOM_LABEL);
            this.jFrame.setTitle(CUSTOM_TITLE);
            this.dropHelper.disableDragAndDrop();
            return true;
        } finally {
            this.runningTask = false;
        }
    }

    private static class UnixPrintStream implements Closeable {
        private final OutputStream outputStream;


        private UnixPrintStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        public void println(String line) {
            try {
                this.outputStream.write((line + '\n').getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws IOException {
            this.outputStream.close();
        }
    }
}
