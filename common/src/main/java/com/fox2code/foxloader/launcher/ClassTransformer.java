package com.fox2code.foxloader.launcher;

import org.objectweb.asm.Opcodes;

public interface ClassTransformer {
    int ASM_BUILD = Opcodes.ASM9;

    byte[] transform(byte[] bytes,String className);
}
