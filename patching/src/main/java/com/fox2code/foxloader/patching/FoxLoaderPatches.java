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
