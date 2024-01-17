package com.fox2code.foxloader.launcher;

import org.objectweb.asm.Opcodes;

/**
 * Used to transform classes, if a same instance also implement {@link ClassGenerator},
 * {@link #transform(byte[], String)} won't be called for classes the instance generate.
 */
@FunctionalInterface
public interface ClassTransformer {
    // Note: Opcodes is not accessible from this class
    // It's oki to do so there cause the value is inlined at compile time
    int ASM_BUILD = Opcodes.ASM9;

    byte[] transform(byte[] bytes,String className);
}
