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
package com.fox2code.foxloader.patching.dev;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class DevelopmentSourcePatcher {
    public static void unpickPatchedJar(File patchedJar, File unpickedJar) throws IOException {
        DevelopmentSourceConstantData developmentSourceConstantData =
                DevelopmentSourceConstantData.fromPatchedJar(patchedJar);

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
                    DevelopmentSourceTransformer.patchForDev(developmentSourceConstantData, classNode);
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
