package com.fox2code.foxloader.patching.dev;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class DevelopmentSourcePatcher {
    public static void unpickPatchedJar(File patchedJar, File unpickedJar) throws IOException {
        /* WIP: try (JarFile jarFile = new JarFile(patchedJar)) {
            JarEntry jarEntry = jarFile.getJarEntry("");
        } / will be for better decomp later */

        try(ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(patchedJar.toPath()));
            ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(unpickedJar.toPath()))) {
            zipOutputStream.setLevel(9);
            ZipEntry zipEntry;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(131072);
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String path = zipEntry.getName();
                zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                if (path.endsWith(".class")) {
                    byteArrayOutputStream.reset();
                    copy(zipInputStream, byteArrayOutputStream);
                    ClassReader classReader = new ClassReader(byteArrayOutputStream.toByteArray());
                    ClassNode classNode = new ClassNode();
                    classReader.accept(classNode, ClassReader.SKIP_FRAMES);
                    DevelopmentSourceTransformer.patchForDev(classNode);
                    ClassWriter classWriter = new ClassWriter(0);
                    classNode.accept(classWriter);
                    byte[] compiled = classWriter.toByteArray();
                    zipOutputStream.write(compiled);
                    zipOutputStream.closeEntry();
                } else {
                    copy(zipInputStream, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
            zipOutputStream.finish();
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
