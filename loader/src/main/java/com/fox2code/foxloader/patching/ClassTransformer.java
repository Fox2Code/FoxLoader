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

    @Nullable public abstract ClassNode transform(@Nullable FileInfo container,@Nullable ClassNode classNode,@NotNull String className);
}
