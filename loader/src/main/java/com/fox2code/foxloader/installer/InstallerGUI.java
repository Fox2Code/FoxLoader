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
package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.LauncherType;
import com.fox2code.foxloader.utils.Platform;
import com.fox2code.foxloader.utils.StackTraceStringifier;
import com.fox2code.foxloader.utils.io.CertificateHelper;
import com.fox2code.foxloader.utils.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class InstallerGUI {
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
    private static final HashSet<String> MMC_PATCHES_ALL = new HashSet<>(Arrays.asList(
            "com.fox2code.foxloader.json", "net.minecraft.json",
            "net.minecraftforge.json", "com.fox2code.lwjglx.json"));
    private static final String[] MMC_FILES = new String[]{"patches/com.fox2code.foxloader.json",
            "patches/net.minecraft.json", "patches/net.minecraftforge.json",
            "patches/org.lwjgl.json", "instance.cfg", "mmc-pack.json"};
    private static final String[] MMC_FILES_LWJGLX = new String[]{"patches/com.fox2code.foxloader.json",
            "patches/net.minecraft.json", "patches/net.minecraftforge.json",
            "patches/com.fox2code.lwjglx.json", "instance.cfg", "mmc-pack-lwjglx.json"};
    private final InstallerPlatform installerPlatform;
    private final LauncherType launcherType;
    private final JFrame jFrame;
    private final Dimension minDimensions;
    private final JPanel globalContainer;
    private final JLabel label;
    private final JProgressBar progressBar;
    private boolean runningTask;
    private final String versionName;

    public InstallerGUI(InstallerPlatform installerPlatform) {
        DependencyHelper.DependencyImpl.install(InstallerDependencyHelperImpl.INSTANCE);
        DependencyHelper.setMCLibraryRoot(new File(Platform.getAppDir("minecraft"), "libraries"));
        CertificateHelper.initializeSafe();
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
        JPanel languageContainer = makeContainer("installer.language");
        languageContainer.add(TranslateEngine.makeLanguageSelectComponent());
        JPanel clientContainer = makeContainer("installer.install-client");
        JButton minecraftButton;
        if (installerPlatform.specialLauncher) {
            minecraftButton = makeButton(clientContainer,
                    "installer.install-special", this::installMineCraft, installerPlatform.platformName);
        } else {
            minecraftButton = makeButton(clientContainer,
                    "installer.install-minecraft", this::installMineCraft);
            makeButton(clientContainer,
                    "installer.extract-multimc", () -> this.extractMMCInstance(false));
            makeButton(clientContainer,
                    "installer.extract-multimc-lwjglx", () -> this.extractMMCInstance(true));
        }
        if (BuildConfig.IS_PRIVATE_DEV_BUILD) {
            minecraftButton.setEnabled(false);
        }
        JButton serverButton = null;
        if (this.installerPlatform.fullscreenLayout) {
            makeButton(clientContainer, "installer.exit-installer", this::exitInstaller);
        } else {
            JPanel serverContainer = makeContainerEx("installer.install-server", true);
            serverButton = makeButton(serverContainer, "installer.extract-server", this::extractServer);
            serverContainer.add(new SelectableTranslatableLabel("installer.install-server.text.*"));
            if (BuildConfig.IS_PRIVATE_DEV_BUILD) {
                serverButton.setEnabled(false);
            }
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
        DependencyHelper.DependencyImpl.install(InstallerDependencyHelperImpl.INSTANCE);
        DependencyHelper.setMCLibraryRoot(new File(Platform.getAppDir("minecraft"), "libraries"));
        CertificateHelper.initializeSafe();
        this.installerPlatform = installerPlatform;
        this.launcherType = launcherType;
        versionName = DEFAULT_VERSION_NAME;
        jFrame = null;
        minDimensions = null;
        globalContainer = null;
        label = null;
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
        this.progressBar.setValue(0);
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

    public void extractMMCInstance(boolean lwjglx) {
        String fileName = Main.currentInstallerFile.getName();
        if (fileName.endsWith("-installer.jar")) {
            fileName = fileName.replace("-installer.jar", ".jar");
        }
        this.extractMMCInstance(fileName, lwjglx);
    }

    public void extractMMCInstance(String baseFileName, boolean lwjglx) {
        if (this.checkInstaller(true)) {
            return;
        }
        File privateDevBuildSlimJarInput = null;
        if (BuildConfig.IS_PRIVATE_DEV_BUILD) {
            privateDevBuildSlimJarInput = DependencyHelper.getLocalSlimReIndevJarFile();
        }

        File instanceDest = new File(Main.currentInstallerFile.getParentFile(),
                baseFileName.substring(0, baseFileName.length() - 4) +
                        (lwjglx ? "-mmc-lwjglx.zip" : "-mmc.zip"));
        String lwjglPatchId, lwjglVersion;
        if (lwjglx) {
            lwjglVersion = "3.3.3";
            lwjglPatchId = "org.lwjgl3";
        } else {
            lwjglVersion = "2.9.4-nightly-20150209";
            lwjglPatchId = "org.lwjgl";
        }
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(instanceDest.toPath()))) {
            zipOutputStream.putNextEntry(new ZipEntry("libraries/foxloader-" + BuildConfig.FOXLOADER_VERSION + ".jar"));
            copyCloseIn(Files.newInputStream(Main.currentInstallerFile.toPath()), zipOutputStream);
            zipOutputStream.closeEntry();
            if (privateDevBuildSlimJarInput != null) {
                zipOutputStream.putNextEntry(new ZipEntry("libraries/" + BuildConfig.SLIM_JAR_NAME));
                copyCloseIn(Files.newInputStream(privateDevBuildSlimJarInput.toPath()), zipOutputStream);
                zipOutputStream.closeEntry();
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (String entry : (lwjglx ? MMC_FILES_LWJGLX : MMC_FILES)) {
                zipOutputStream.putNextEntry(new ZipEntry(
                        "mmc-pack-lwjglx.json".equals(entry) ?
                                "mmc-pack.json" : entry));
                byteArrayOutputStream.reset();
                IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                        "/mmc/" + entry), byteArrayOutputStream);
                copyCloseIn(new ByteArrayInputStream(byteArrayOutputStream.toString()
                        .replace("#version#", this.versionName)
                        .replace("#foxloader_version#", BuildConfig.FOXLOADER_VERSION)
                        .replace("#lwjglx_version#", BuildConfig.LWJGLX_VERSION)
                        .replace("#lwjgl_version#", lwjglVersion)
                        .replace("#lwjgl_uid#", lwjglPatchId)
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
        String fileName = Main.currentInstallerFile.getName();
        if (fileName.endsWith("-installer.jar")) {
            fileName = fileName.replace("-installer.jar", ".jar");
        }
        this.extractServer(fileName);
    }

    public void extractServer(String baseFileName) {
        if (this.checkInstaller(true)) {
            return;
        }

        File serverDest = new File(Main.currentInstallerFile.getParentFile(),
                baseFileName.substring(0, baseFileName.length() - 4) + "-server.jar");
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
                    // Insert the original FoxLoader jar hash in the server jar file.
                    zipOutputStream.putNextEntry(new ZipEntry("META-INF/FL-SHA-256"));
                    zipOutputStream.write(IOUtils.sha256Of(Main.currentInstallerFile));
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

    public void doSilentInstall() throws IOException {
        if (this.checkInstaller(false)) {
            return;
        }
        switch (this.launcherType) {
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
                                !MMC_PATCHES_ALL.contains(file.getName())) {
                            if (!file.delete()) file.deleteOnExit();
                        }
                    }
                }
                File patchLwjglx = new File(patches, "com.fox2code.lwjglx.json");
                boolean lwjglx = patchLwjglx.exists();
                String lwjglPatchId, lwjglVersion;
                if (lwjglx) {
                    lwjglVersion = "3.3.3";
                    lwjglPatchId = "org.lwjgl3";
                } else {
                    lwjglVersion = "2.9.4-nightly-20150209";
                    lwjglPatchId = "org.lwjgl";
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                        "/mmc/patches/com.fox2code.foxloader.json"), byteArrayOutputStream);
                IOUtils.copyAndClose(new ByteArrayInputStream(byteArrayOutputStream.toString()
                        .replace("#version#", this.versionName)
                        .replace("#foxloader_version#", BuildConfig.FOXLOADER_VERSION)
                        .replace("#lwjgl_version#", lwjglVersion)
                        .replace("#lwjgl_uid#", lwjglPatchId)
                        .getBytes(StandardCharsets.UTF_8)), Files.newOutputStream(patch.toPath()));
                if (lwjglx) {
                    byteArrayOutputStream.reset();
                    IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                            "/mmc/patches/com.fox2code.lwjglx.json"), byteArrayOutputStream);
                    IOUtils.copyAndClose(new ByteArrayInputStream(byteArrayOutputStream.toString()
                            .replace("#lwjglx_version#", BuildConfig.LWJGLX_VERSION)
                            .getBytes(StandardCharsets.UTF_8)), Files.newOutputStream(patchLwjglx.toPath()));
                }
                for (String entry : new String[]{ // Fix in place replace!
                        "patches/net.minecraft.json", "patches/net.minecraftforge.json"}) {
                    IOUtils.copyAndClose(InstallerGUI.class.getResourceAsStream(
                                    "/mmc/patches/" + entry + ".json"),
                            Files.newOutputStream(new File(entry).toPath()));
                }
                break;
            }
            default: {
                System.out.println("Unsupported launcherType: " + this.launcherType);
                break;
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
}
