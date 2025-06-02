package com.fox2code.foxloader.patching;

import com.fox2code.foxloader.launcher.FileInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public abstract class ClassTransformer implements Opcodes, Comparable<ClassTransformer> {
    public static final int ASM_BUILD = Opcodes.ASM9;
    public static final int ACC_COMPUTE_FRAMES = 0x80000;
    public final long priority;
    final boolean preMixinTransformer;

    public ClassTransformer() {
        this(0, false);
    }

    public ClassTransformer(long priority) {
        this(priority, false);
    }

    public ClassTransformer(long priority, boolean preMixinTransformer) {
        this.priority = priority;
        this.preMixinTransformer = preMixinTransformer;
    }

    @Override
    public int compareTo(@NotNull ClassTransformer o) {
        return Long.compare(this.priority, o.priority);
    }

    @Nullable
    public abstract ClassNode transform(@Nullable FileInfo container,@Nullable ClassNode classNode,@NotNull String className);
}
