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
package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import com.fox2code.foxloader.utils.io.IOUtils;
import com.fox2code.rebuild.ClassDataProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class GamePatches {
    private static final ArrayList<GamePatch> noPatches = new ArrayList<>();
    private static final ArrayList<GamePatch> globalGamePatches = new ArrayList<>();
    private static final HashMap<String, ArrayList<GamePatch>> gameClassPatches = new HashMap<>();
    private static final Function<String, ArrayList<GamePatch>> provider = k -> new ArrayList<>();
    private static final Function<ClassNode, byte[]> classNodeToBytesDefault = classNode -> {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    };
    private static void addGamePatch(GamePatch gamePatch) {
        if (gamePatch.targets == null) {
            globalGamePatches.add(gamePatch);
        } else {
            for (String target : gamePatch.targets) {
                if (!target.isEmpty()) {
                    gameClassPatches.computeIfAbsent(target, provider).add(gamePatch);
                }
            }
        }
    }

    static {
        if (HotfixesPatch.USE_HOTFIXES) {
            addGamePatch(new HotfixesPatch());
        }
        addGamePatch(new RegistryPatch());
        addGamePatch(new ClientGameDirectoryPatch());
        addGamePatch(new NetworkConnectionPatch());
        addGamePatch(new KeyBindingPatch());
        addGamePatch(new TickEventPatch());
        addGamePatch(new TranslationPatch());
        addGamePatch(new GuiElementsPatch());
        addGamePatch(new GuiScreenPatch());
        addGamePatch(new CommandGamePatch());
        addGamePatch(new ChattingPatch());
        addGamePatch(new ExplosionPatch());
        addGamePatch(new PlayerInteractionsPatch());
        addGamePatch(new TexturesPatch());
        addGamePatch(new RecipesPatch());
        addGamePatch(new FluidsPatch());
        addGamePatch(new CertificateHelperPatch());
        addGamePatch(new EntitiesPatch());
        addGamePatch(new RenderPatch());
        addGamePatch(new LifecyclePatch());
        addGamePatch(new ClientCommandsPatch());
        addGamePatch(new LogAgentPatch());
        addGamePatch(new ContainerPatch());
        addGamePatch(new TileEntityPatch());
        addGamePatch(new EditTextPatch());
        addGamePatch(new PlayerSelectionPatch());
        addGamePatch(new MovementPatch());
        addGamePatch(new LootPatch());
        // Debug & cosmetic stuff
        addGamePatch(new VarNamePatch());
    }

    public static ClassNode patchClassNode(ClassNode classNode) {
        for (GamePatch gamePatch : gameClassPatches.getOrDefault(classNode.name, noPatches)) {
            classNode = gamePatch.transform(classNode);
            if (classNode == null) return null;
        }
        for (GamePatch gamePatch : globalGamePatches) {
            classNode = gamePatch.transform(classNode);
            if (classNode == null) return null;
        }
        return classNode;
    }

    public static void patchSlimJarDev(File slimJar, File patchedJar) throws IOException {
        File parent = patchedJar.getParentFile();
        if (parent.isDirectory()) {
            for (File file : Objects.requireNonNull(parent.listFiles())) {
                if (file.exists() && !file.delete()) {
                    throw new IOException("Failed to delete " + file.getName());
                }
            }
        }
        if (parent.exists() && !parent.delete()) {
            throw new IOException("Failed to delete patch folder");
        }
        if (!parent.mkdirs()) {
            throw new IOException("Failed to make patch folder");
        }
        boolean failedRename = false;
        try {
            patchSlimJarImpl(slimJar, null, patchedJar, true, true);
        } finally {
            if (!patchedJar.renameTo(patchedJar)) {
                failedRename = true;
            }
        }
        if (failedRename) {
            throw new IOException("Failed to rename " + patchedJar.getName() + " to " + patchedJar.getName());
        }
    }

    public static void patchSlimJar(File slimJar, File patchedJar) throws IOException {
        patchSlimJarImpl(slimJar, null, patchedJar, false, false);
    }

    public static void patchSlimJarComputeFrames(File slimJar, File patchedJar) throws IOException {
        patchSlimJarImpl(slimJar, null, patchedJar, false, true);
    }

    public static void patchSlimJarWithCoreMods(File slimJar, List<File> coreMods, File patchedJar) throws IOException {
        if (coreMods == null || coreMods.isEmpty()) {
            patchSlimJar(slimJar, patchedJar);
            return;
        }
        try (JarSourceSet jarSourceSet = new JarSourceSet(coreMods)) {
            patchSlimJarImpl(slimJar, jarSourceSet, patchedJar, false, false);
        }
    }

    private static void patchSlimJarImpl(File slimJar,JarSourceSet jarSourceSet, File patchedJar,
                                         boolean check, boolean computeFrames) throws IOException {
        Function<ClassNode, byte[]> classNodeToBytes = classNodeToBytesDefault;
        if (computeFrames) {
            classNodeToBytes = ReBuildHelper.makeBasicFrameComputeWithJar(slimJar);
        }
        HashSet<String> classesToPatch = new HashSet<>(gameClassPatches.keySet());
        try(ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(slimJar.toPath()));
            ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(patchedJar.toPath()))) {
            zipOutputStream.setLevel(9);
            ZipEntry zipEntry;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(131072);
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String path = zipEntry.getName();
                InputStream jarSourceInputStream = jarSourceSet != null ?
                        jarSourceSet.getInputStreamParsed(path) : null;
                patchAndInsert(byteArrayOutputStream, classNodeToBytes, classesToPatch, zipOutputStream,
                        jarSourceInputStream == null ? zipInputStream : jarSourceInputStream,
                        path, check, jarSourceInputStream != null);
            }
            if (jarSourceSet != null) {
                while ((zipEntry = jarSourceSet.nextExtraZipEntry()) != null) {
                    String path = zipEntry.getName();
                    InputStream inputStream = jarSourceSet.getInputStreamOfCurrentEntry();
                    patchAndInsert(byteArrayOutputStream, classNodeToBytes, classesToPatch, zipOutputStream,
                            inputStream, path, check, true);
                }
            }
            zipOutputStream.finish();
        }
        if (!classesToPatch.isEmpty()) {
            throw new IOException("Missing classes to patch: " + classesToPatch);
        }
    }

    private static void patchAndInsert(
            ByteArrayOutputStream byteArrayOutputStream,
            Function<ClassNode, byte[]> classNodeToBytes,
            HashSet<String> classesToPatch,
            ZipOutputStream zipOutputStream, InputStream inputStream,
            String path, boolean check, boolean closeInput) throws IOException {
        if (path.endsWith(".class")) {
            byteArrayOutputStream.reset();
            IOUtils.copy(inputStream, byteArrayOutputStream);
            if (closeInput) {
                inputStream.close();
            }
            ClassReader classReader = new ClassReader(byteArrayOutputStream.toByteArray());
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.SKIP_FRAMES);
            classesToPatch.remove(classNode.name);
            classNode = patchClassNode(classNode);
            if (classNode != null) {
                zipOutputStream.putNextEntry(new ZipEntry(path));
                byte[] compiled;
                RuntimeException delayedException = null;
                try {
                    compiled = classNodeToBytes.apply(classNode);
                } catch (RuntimeException delayed) {
                    delayedException = delayed;
                    ClassWriter classWriter = new ClassWriter(0);
                    classNode.accept(classWriter);
                    compiled = classWriter.toByteArray();
                }
                zipOutputStream.write(compiled);
                zipOutputStream.closeEntry();
                if (delayedException != null) throw delayedException;
                if (check) TransformerUtils.checkBytecodeValidity(compiled);
            }
        } else {
            zipOutputStream.putNextEntry(new ZipEntry(path));
            IOUtils.copy(inputStream, zipOutputStream);
            if (closeInput) {
                inputStream.close();
            }
            zipOutputStream.closeEntry();
        }
    }

    private static final class JarSourceSet implements Closeable {
        private final List<ZipFile> zipFiles;
        private final HashSet<String> parsedFiles;
        private final Iterator<ZipFile> zipFileIterator;
        private ZipFile currentZipFile;
        private Enumeration<? extends ZipEntry> zipEntryEnumeration;
        private ZipEntry currentZipEntry;

        private JarSourceSet(List<File> files) throws IOException {
            this.zipFiles = new ArrayList<>();
            try {
                for (File file : files) {
                    this.zipFiles.add(new ZipFile(file));
                }
            } catch (IOException ioe) {
                try {
                    this.close();
                } catch (IOException ignored) {}
                throw ioe;
            }
            this.parsedFiles = new HashSet<>();
            this.zipFileIterator = this.zipFiles.iterator();
        }

        public InputStream getInputStreamParsed(String path) throws IOException {
            this.parsedFiles.add(path);
            for (ZipFile zipFile : this.zipFiles) {
                ZipEntry zipEntry = zipFile.getEntry(path);
                if (zipEntry != null) {
                    return zipFile.getInputStream(zipEntry);
                }
            }
            return null;
        }

        public ZipEntry nextExtraZipEntry() {
            while (true) {
                while (this.zipEntryEnumeration == null ||
                        !this.zipEntryEnumeration.hasMoreElements()) {
                    if (!this.zipFileIterator.hasNext()) {
                        return null;
                    }
                    this.currentZipFile = this.zipFileIterator.next();
                    this.zipEntryEnumeration = this.currentZipFile.entries();
                }
                while (this.zipEntryEnumeration.hasMoreElements()) {
                    ZipEntry zipEntry = this.zipEntryEnumeration.nextElement();
                    if (this.parsedFiles.add(zipEntry.getName())) {
                        return this.currentZipEntry = zipEntry;
                    }
                }
            }
        }

        public InputStream getInputStreamOfCurrentEntry() throws IOException {
            return this.currentZipFile.getInputStream(this.currentZipEntry);
        }

        @Override
        public void close() throws IOException {
            for (ZipFile zipFile : this.zipFiles) {
                zipFile.close();
            }
            this.zipFiles.clear();
        }
    }

    private static class ReBuildHelper {
        public static Function<ClassNode, byte[]> makeBasicFrameComputeWithJar(File jarFile) throws IOException {
            final ClassDataProvider classDataProvider = new ClassDataProvider(
                    new URLClassLoader(new URL[]{jarFile.toURI().toURL()}));
            return classNode -> {
                ClassWriter classWriter = classDataProvider.newClassWriter();
                classNode.accept(classWriter);
                return classWriter.toByteArray();
            };
        }
    }
}
