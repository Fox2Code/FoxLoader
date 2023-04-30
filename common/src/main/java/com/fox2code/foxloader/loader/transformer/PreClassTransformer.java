package com.fox2code.foxloader.loader.transformer;

import com.fox2code.foxloader.launcher.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public interface PreClassTransformer extends ClassTransformer, Opcodes {
    default byte[] transform(byte[] bytes,String className) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        transform(classNode, className);
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    default boolean changeClassStructure() {
        return false;
    }

    void transform(ClassNode classNode, String className);
}
