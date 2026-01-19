/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * GamePatch that are registered at {@link GamePatches}
 */
abstract class GamePatch implements Opcodes {
    static final Void ALL_CLASSES = null;
    public static final String LAMBDA_ARGS = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
    public static final String String = "java/lang/String";
    public static final String StringBuilder = "java/lang/StringBuilder";
    public static final String Collection = "java/util/Collection";
    public static final String Set = "java/util/Set";
    public static final int ASM_API = TransformerUtils.ASM_BUILD;
    final String[] targets;

    protected GamePatch(Void ignored) {
        this.targets = null;
    }

    protected GamePatch(String target) {
        this.targets = target == null ? null : new String[]{target};
    }

    protected GamePatch(String[] targets) {
        this.targets = targets == null || targets.length == 0 ? null : targets;
    }

    private void checkAccess() {

    }

    public abstract ClassNode transform(ClassNode classNode);
}
