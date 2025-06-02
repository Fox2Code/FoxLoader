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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.Files;

// Allows us to do stuff java doesn't allow us to do.
public final class FoxLoaderPatches implements Opcodes {
    private static final boolean DEBUG = false;

    public static void main(String[] args) throws IOException {
        patchFoxLoaderClassesDir(new File(args[0]));
    }

    public static void patchFoxLoaderClassesDir(File classesDir) throws IOException {
        patchFoxClassLoader(classesDir);
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
}
