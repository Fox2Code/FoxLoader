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
import org.objectweb.asm.tree.*;

/**
 * Optimize the game after it has been transformed, allows for the optimizations to be mixin aware.
 */
final class LateOptimizationTransformer extends ClassTransformer {
    LateOptimizationTransformer() {
        super(Long.MIN_VALUE);
    }

    @Override
    public @Nullable ClassNode transform(@Nullable FileInfo container, @Nullable ClassNode classNode, @NotNull String className) {
        if (classNode == null) {
            return null;
        }
        for (MethodNode methodNode : classNode.methods) {
            final InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode abstractInsnNode : insnList) {
                // This is to avoid using toArray() for iterating
                abstractInsnNode = abstractInsnNode.getPrevious();
                if (abstractInsnNode == null || abstractInsnNode.getOpcode() != ANEWARRAY) {
                    continue;
                }
                AbstractInsnNode previous = abstractInsnNode.getPrevious();
                if (previous == null || previous.getOpcode() != ICONST_0) {
                    continue;
                }
                String fieldName;
                switch (((TypeInsnNode) abstractInsnNode).desc) {
                    case "java/lang/Object": {
                        fieldName = "EMPTY_OBJECT_ARRAY";
                        break;
                    }
                    case "java/lang/String": {
                        fieldName = "EMPTY_STRING_ARRAY";
                        break;
                    }
                    case "java/lang/Class": {
                        fieldName = "EMPTY_CLASS_ARRAY";
                        break;
                    }
                    default: {
                        continue;
                    }
                }
                insnList.insert(abstractInsnNode, new FieldInsnNode(GETSTATIC,
                        "com/fox2code/foxloader/utils/EmptyArrays", fieldName,
                        "[L" + ((TypeInsnNode) abstractInsnNode).desc + ";"));
                insnList.remove(abstractInsnNode);
                insnList.remove(previous);
            }
        }

        return classNode;
    }
}
