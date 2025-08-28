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
package com.fox2code.foxloader.patching;

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.utils.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.Files;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

// Allows us to do stuff java doesn't allow us to do.
public final class FoxLoaderPatches implements Opcodes {
    private static final boolean DEBUG = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            patchFoxLoaderClassesDir(new File(args[0]));
            patchFoxLoaderResourcesDir(new File(args[1]));
        } else {
            patchFoxLoaderFinalJar(new File(args[0]));
        }
    }

    public static void patchFoxLoaderClassesDir(File classesDir) throws IOException {
        patchFoxClassLoader(classesDir);
    }

    public static void patchFoxLoaderResourcesDir(File resourcesDir) throws IOException {
        generateDependenciesResources(resourcesDir);
    }

    // Patching this late allow FoxLoader to inline BuildConfig values while having mods not inline them.
    public static void patchFoxLoaderFinalJar(File foxloaderJar) throws IOException {
        File backup = new File(foxloaderJar.getParentFile(), foxloaderJar.getName() + ".bak");
        if (backup.exists() && !backup.delete()) {
            throw new IOException("Failed to delete backup jar");
        }
        if (!foxloaderJar.renameTo(backup)) {
            throw new IOException("Failed to rename loader jar");
        }
        boolean patchedBuildConfig = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(backup.toPath())));
             ZipOutputStream zipOutputStream = new ZipOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(foxloaderJar.toPath())))) {
            zipOutputStream.setLevel(9);
            ZipEntry input;
            while ((input = zipInputStream.getNextEntry()) != null) {
                if (input.isDirectory()) continue;
                String pathName = input.getName();
                ZipEntry output = new ZipEntry(pathName);
                zipOutputStream.putNextEntry(output);
                if ("com/fox2code/foxloader/launcher/BuildConfig.class".equals(pathName)) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(zipInputStream, byteArrayOutputStream);
                    zipOutputStream.write(patchBuildConfig(byteArrayOutputStream.toByteArray()));
                    patchedBuildConfig = true;
                } else {
                    IOUtils.copy(zipInputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
                zipInputStream.closeEntry();
            }
            zipOutputStream.finish();
        }
        if (!patchedBuildConfig) {
            throw new IOException("Patching BuildConfig failed.");
        }
        if (backup.exists() && !backup.delete()) {
            throw new IOException("Failed to delete backup jar");
        }
    }

    private static byte[] patchBuildConfig(byte[] buildConfig) {
        ClassNode classNode = new ClassNode();
        new ClassReader(buildConfig).accept(classNode, 0);
        TransformerUtils.deInlineFieldConstants(classNode, true);
        if (classNode.methods.size() != 2) {
            throw new RuntimeException("WHAT? " + classNode.methods.size());
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static void patchFoxClassLoader(File classesDir) throws IOException {
        File foxClassLoader = new File(classesDir, "com/fox2code/foxloader/launcher/FoxClassLoader.class");
        File source = foxClassLoader;
        if (DEBUG) {
            File foxClassLoaderBak = new File(classesDir, "com/fox2code/foxloader/launcher/FoxClassLoader.bak.class");
            if (!foxClassLoaderBak.exists()) {
                if (!foxClassLoader.exists()) {
                    throw new IOException("File do not exists " + foxClassLoader);
                }
                if (!foxClassLoader.renameTo(foxClassLoaderBak)) {
                    throw new IOException("Failed to rename " + foxClassLoader + " to " + foxClassLoaderBak);
                }
            }
            source = foxClassLoaderBak;
        }
        ClassNode classNode = new ClassNode();
        ClassReader classReader;
        try (FileInputStream fileInputStream = new FileInputStream(source)) {
            classReader = new ClassReader(new BufferedInputStream(fileInputStream));
            classReader.accept(classNode, 0);
        }
        if (classNode.name == null) throw new RuntimeException("Name is null");
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "<init>", "(Ljava/lang/String;)V");
        AbstractInsnNode nextInsn = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                if (methodInsnNode.owner.equals("java/net/URLClassLoader")) {
                    if (methodInsnNode.desc.startsWith("(Ljava/lang/String;")) return; // Skip if already patched
                    methodInsnNode.desc = methodInsnNode.desc.replace("(", "(Ljava/lang/String;");
                    nextInsn = methodInsnNode;
                    break;
                }
            }
        }
        int goBackBy = 4;
        while (goBackBy -- > 0) {
            nextInsn = TransformerUtils.previousCodeInsn(nextInsn);
        }
        methodNode.instructions.insertBefore(nextInsn, new VarInsnNode(ALOAD, 1));
        methodNode.maxStack++;
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        classNode.accept(classWriter);
        byte[] classData = classWriter.toByteArray();
        TransformerUtils.checkBytecodeValidity(classData, true);
        Files.write(foxClassLoader.toPath(), classData);
    }

    private static void patchEarlyEnum(File classesDir, String className) throws IOException {
        String asmName = className.replace('.', '/');
        File enumClassFile = new File(classesDir, asmName + ".class");
        ClassNode classNode = new ClassNode();
        ClassReader classReader;
        try (FileInputStream fileInputStream = new FileInputStream(enumClassFile)) {
            classReader = new ClassReader(new BufferedInputStream(fileInputStream));
            classReader.accept(classNode, 0);
        }
        if (classNode.name == null) throw new RuntimeException("Name is null");
        TransformerUtils.patchInValue$(classNode);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        classNode.accept(classWriter);
        byte[] classData = classWriter.toByteArray();
        TransformerUtils.checkBytecodeValidity(classData, true);
        Files.write(enumClassFile.toPath(), classData);
    }

    private static void generateDependenciesResources(File resourcesDir) throws IOException {
        File metaInf = new File(resourcesDir, "META-INF");
        if (!metaInf.isDirectory() && !metaInf.mkdirs()) {
            throw new IOException("Cannot create META-INF directory");
        }
        generateDependenciesResource(new File(metaInf, "DEPENDENCIES"),
                DependencyHelper.commonDependencies);
        // Add modern dependencies for Unimined support
        generateDependenciesResource(new File(metaInf, "DEPENDENCIES_MODERN"),
                DependencyHelper.commonDependenciesModernJava);
        // Also add modern dependencies for dependency bundle feature on Unimined
        for (String dependencyBundle : DependencyHelper.availableDependencyBundles) {
            generateDependenciesResource(
                    new File(metaInf, "DEPENDENCIES_BUNDLE_" +
                            (dependencyBundle.toUpperCase(Locale.ROOT))),
                    DependencyHelper.getDependencyBundle(dependencyBundle));
        }
    }

    private static void generateDependenciesResource(
            File file, DependencyHelper.Dependency[] dependencies) throws IOException {
        try (PrintStream printStream = new PrintStream(file, "UTF-8")) {
            printStream.println("# Autogenerated by " + BuildConfig.FOXLOADER_DISPLAY);
            for (DependencyHelper.Dependency dependency : dependencies) {
                printStream.println(dependency.javaSupport + " " + dependency.name);
            }
        }
    }
}
