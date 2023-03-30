package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.DependencyHelper;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.launcher.StackTraceStringifier;
import com.fox2code.foxloader.launcher.utils.Platform;
import com.fox2code.foxloader.launcher.utils.SourceUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class InstallerGUI implements FileDropHelper.FileDropHandler {
    private static final String DEFAULT_VERSION_NAME =
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
    private final InstallerPlatform installerPlatform;
    private final LauncherType launcherType;
    private final JFrame jFrame;
    private final Dimension minDimensions;
    private final JPanel globalContainer;
    private final JLabel label;
    private final FileDropHelper dropHelper;
    private final JButton minecraftButton;
    private final JButton mmcButton;
    private final JProgressBar progressBar;
    private boolean runningTask;
    private File reIndevSource;
    private String versionName;

    public InstallerGUI(InstallerPlatform installerPlatform) {
        this.installerPlatform = installerPlatform;
        this.launcherType = null;
        versionName = DEFAULT_VERSION_NAME;
        jFrame = new JFrame(DEFAULT_TITLE);
        jFrame.setMinimumSize(minDimensions = new Dimension(230, 30));
        if (installerPlatform.fullscreen) {
            jFrame.setUndecorated(true);
        }
        globalContainer = makeContainer(null);
        label = new JLabel(installerPlatform.fullscreenLayout ? FULLSCREEN_LABEL : DEFAULT_LABEL);
        dropHelper = new FileDropHelper(globalContainer, this);
        JPanel clientContainer = makeContainer("Install Client");
        JButton minecraftButton;
        JButton mmcButton;
        if (installerPlatform.specialLauncher) {
            minecraftButton = makeButton(clientContainer,
                    "Install on " + installerPlatform.platformName + " Launcher", this::installMineCraft);
            mmcButton = null;
        } else {
            minecraftButton = makeButton(clientContainer,
                    "Install on Minecraft Launcher", this::installMineCraft);
            makeButton(clientContainer,
                    "Install on BetaCraft Launcher", this::installBetaCraft);
            mmcButton = makeButton(clientContainer,
                    "Extract MultiMC Instance", this::extractMMCInstance);
        }
        this.mmcButton = mmcButton;
        this.minecraftButton = minecraftButton;
        if (this.installerPlatform.fullscreenLayout) {
            makeButton(clientContainer, "Exit installer", this::exitInstaller);
        } else {
            JPanel serverContainer = makeContainer("Install Server");
            // makeButton(serverContainer, "Install modded server here!", null);
            serverContainer.add(new Label("You can add \"--server\" argument to"));
            serverContainer.add(new Label("the installer to run the server."));
        }
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
        jFrame = null;
        minDimensions = null;
        globalContainer = null;
        label = null;
        dropHelper = null;
        minecraftButton = null;
        mmcButton = null;
        progressBar = new JProgressBar();
        progressBar.setMaximum(PROGRESS_BAR_MAX);
    }

    private JPanel makeContainer(final String text) {
        final JPanel container = new JPanel();
        if (text != null) {
            container.setLayout(installerPlatform.fullscreenLayout ?
                    new FlowLayout(FlowLayout.CENTER) : new GridLayout(0, 1, 0, 3));
            container.setBorder(BorderFactory.createTitledBorder(text));
            globalContainer.add(container);
        } else {
            container.setLayout(installerPlatform.fullscreenLayout ?
                    new VerticalGridBagLayout() : new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        }
        return container;
    }

    private JButton makeButton(final JPanel panel,final String text,final Runnable action) {
        final JButton button = new JButton(text);
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

    private boolean checkInstaller() {
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
            showMessage("Please run the compiled jar file to test the installation process", true);
            return true;
        }
        if (!Main.currentInstallerFile.getName()
                .toLowerCase(Locale.ROOT).endsWith(".jar") &&
                !this.installerPlatform.specialLauncher) {
            if (this.jFrame == null) return true;
            JOptionPane.showMessageDialog(this.jFrame,
                    "Why the file is not a \".jar\"??? TELL ME!!! WHY???",
                    this.jFrame.getTitle(), JOptionPane.QUESTION_MESSAGE);
            return true;
        }
        return false;
    }

    public void installMineCraft() {
        if (this.checkInstaller()) {
            return;
        }
        if (this.installerPlatform.specialLauncher) {
            DependencyHelper.setMCLibraryRoot();
            for (int i = 0; i < DependencyHelper.commonDependencies.length; i++) {
                DependencyHelper.loadDependency(DependencyHelper.commonDependencies[i]);
                progressBar.setValue(i + 1);
            }
        }
        File minecraft = Platform.getAppDir("minecraft");
        File versions = new File(minecraft, "versions");
        File foxLoaderVersion = new File(versions, this.versionName);
        File foxLoaderVersionJar = new File(foxLoaderVersion, this.versionName + ".jar");
        File foxLoaderVersionJson = new File(foxLoaderVersion, this.versionName + ".json");
        if (!foxLoaderVersion.isDirectory() && !foxLoaderVersion.mkdirs()) {
            showMessage("Unable to create version target directory!", true);
            return;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            copyAndClose(InstallerGUI.class.getResourceAsStream(
                    "/launcher-version.json"), byteArrayOutputStream);
            copyAndClose(new ByteArrayInputStream(byteArrayOutputStream.toString()
                            .replace(installerPlatform.specialLauncher ?
                                    "#hack_releaseTime#" : "#no-op#", "releaseTime")
                            .replace("#version#", this.versionName).getBytes(StandardCharsets.UTF_8)),
                    Files.newOutputStream(foxLoaderVersionJson.toPath()));
            Files.copy(Main.currentInstallerFile.toPath(),
                    foxLoaderVersionJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            showError(e);
            return;
        }
        progressBar.setValue(PROGRESS_BAR_MAX);
        showMessage("FoxLoader " + BuildConfig.FOXLOADER_VERSION + " for ReIndev " +
                BuildConfig.REINDEV_VERSION + " has been successfully installed!" +
                USER_INSTRUCTION, false);
    }

    public void installBetaCraft() {
        if (this.checkInstaller()) {
            return;
        }
        File betaCraft = Platform.getAppDir("betacraft");
        File versions = new File(betaCraft, "versions");
        File versionsJsons = new File(versions, "jsons");
        File launchMethods = new File(betaCraft,
                "launcher" + File.separator + "launch-methods");
        if ((!versionsJsons.isDirectory() && !versionsJsons.mkdirs()) ||
                (!launchMethods.isDirectory() && !launchMethods.mkdirs())) {
            showMessage("Unable to create betacraft target directory!", true);
            return;
        }
        File foxLoaderVersionJar = new File(versions, this.versionName + ".jar");
        File foxLoaderVersionJson = new File(versionsJsons, this.versionName + ".info");
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
            copyAndClose(InstallerGUI.class.getResourceAsStream(
                    "/betacraft-" + BuildConfig.FOXLOADER_VERSION + ".jar"),
                    Files.newOutputStream(foxLoaderBetaCraft.toPath()));
        } catch (IOException e) {
            showError(e);
            return;
        }
        progressBar.setValue(PROGRESS_BAR_MAX);
        showMessage("FoxLoader " + BuildConfig.FOXLOADER_VERSION +
                " for ReIndev " + BuildConfig.REINDEV_VERSION + " has been successfully installed!" +
                USER_INSTRUCTION, false);
    }

    public void extractMMCInstance() {
        if (this.checkInstaller()) {
            return;
        }

        String fileName = Main.currentInstallerFile.getName();
        File instanceDest = new File(Main.currentInstallerFile.getParentFile(),
                fileName.substring(0, fileName.length() - 4) + "-mmc.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(instanceDest.toPath()))) {
            zipOutputStream.putNextEntry(new ZipEntry("libraries/foxloader-" + BuildConfig.FOXLOADER_VERSION + ".jar"));
            copy(Files.newInputStream(Main.currentInstallerFile.toPath()), zipOutputStream);
            zipOutputStream.closeEntry();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (String entry : new String[]{"patches/com.fox2code.foxloader.json", "instance.cfg", "mmc-pack.json"}) {
                zipOutputStream.putNextEntry(new ZipEntry(entry));
                byteArrayOutputStream.reset();
                copyAndClose(InstallerGUI.class.getResourceAsStream(
                        "/mmc/" + entry), byteArrayOutputStream);
                copy(new ByteArrayInputStream(byteArrayOutputStream.toString()
                        .replace("#version#", this.versionName)
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
        showMessage("FoxLoader " + BuildConfig.FOXLOADER_VERSION +
                " for ReIndev " + BuildConfig.REINDEV_VERSION + " MMC Instance has been successfully extracted!\n" +
                "(The file should be a \".zip\" next to the installer)\n\n" +
                "To import a zip: Add Instance -> Import from zip -> Browse", false);
    }

    public void doSilentInstall() throws IOException {
        if (this.checkInstaller()) {
            return;
        }
        switch (this.launcherType) {
            default: {
                System.out.println("Unsupported launcherType: " + this.launcherType);
                return;
            }
            case BETA_CRAFT: {
                this.installBetaCraft();
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
                File patches = new File(Main.currentInstallerFile.getParentFile().getParentFile(), "patches");
                File patch = new File(patches, "com.fox2code.foxloader.json");
                if (!patch.exists()) {
                    System.exit(-1);
                    return;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                copyAndClose(InstallerGUI.class.getResourceAsStream(
                        "/mmc/patches/com.fox2code.foxloader.json"), byteArrayOutputStream);
                copyAndClose(new ByteArrayInputStream(byteArrayOutputStream.toString()
                        .replace("#version#", this.versionName)
                        .replace("#foxloader_version#", BuildConfig.FOXLOADER_VERSION)
                        .getBytes(StandardCharsets.UTF_8)), Files.newOutputStream(patch.toPath()));
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

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (InputStream is = inputStream) {
            byte[] byteChunk = new byte[4096];
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                outputStream.write(byteChunk, 0, n);
            }
        }
    }

    private static void copyAndClose(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (InputStream is = inputStream;
             OutputStream out = outputStream) {
            byte[] byteChunk = new byte[4096];
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                out.write(byteChunk, 0, n);
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
                            "Provided jar file is not a ReIndev client!");
                    return false;
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this.jFrame,
                        "Provided file is not a valid jar file!");
                return false;
            }

            this.minecraftButton.setEnabled(false);
            this.mmcButton.setEnabled(false);
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
