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
package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class GamePatches {
    private static final ArrayList<GamePatch> noPatches = new ArrayList<>();
    private static final ArrayList<GamePatch> globalGamePatches = new ArrayList<>();
    private static final HashMap<String, ArrayList<GamePatch>> gameClassPatches = new HashMap<>();
    private static final Function<String, ArrayList<GamePatch>> provider = k -> new ArrayList<>();
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
            patchSlimJarImpl(slimJar, patchedJar, true);
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
        patchSlimJarImpl(slimJar, patchedJar, false);
    }

    private static void patchSlimJarImpl(File slimJar, File patchedJar, boolean check) throws IOException {
        HashSet<String> classesToPatch = new HashSet<>(gameClassPatches.keySet());
        try(ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(slimJar.toPath()));
            ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(patchedJar.toPath()))) {
            zipOutputStream.setLevel(9);
            ZipEntry zipEntry;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(131072);
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String path = zipEntry.getName();
                if (path.endsWith(".class")) {
                    byteArrayOutputStream.reset();
                    copy(zipInputStream, byteArrayOutputStream);
                    ClassReader classReader = new ClassReader(byteArrayOutputStream.toByteArray());
                    ClassNode classNode = new ClassNode();
                    classReader.accept(classNode, ClassReader.SKIP_FRAMES);
                    classesToPatch.remove(classNode.name);
                    classNode = patchClassNode(classNode);
                    if (classNode != null) {
                        zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        classNode.accept(classWriter);
                        byte[] compiled = classWriter.toByteArray();
                        zipOutputStream.write(compiled);
                        zipOutputStream.closeEntry();
                        if (check) TransformerUtils.checkBytecodeValidity(compiled);
                    }
                } else {
                    zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                    copy(zipInputStream, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
            zipOutputStream.finish();
        }
        if (!classesToPatch.isEmpty()) {
            throw new IOException("Missing classes to patch: " + classesToPatch);
        }
    }

    // Utils port for game patches
    static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] byteChunk = new byte[4096];
        int n;

        while ((n = inputStream.read(byteChunk)) > 0) {
            outputStream.write(byteChunk, 0, n);
        }
    }
}
