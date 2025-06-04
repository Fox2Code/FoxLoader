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
        if (className.startsWith("net.minecraft.") &&
                (classNode.access & Opcodes.ACC_ENUM) != 0) {
            TransformerUtils.patchInValue$(classNode);
        }
        for (MethodNode methodNode : classNode.methods) {
            final InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode abstractInsnNode : insnList) {
                OpcodesUtils.getStackConsume(abstractInsnNode);
                OpcodesUtils.getStackProduce(abstractInsnNode);
                // This is to avoid using toArray() for iterating
                abstractInsnNode = abstractInsnNode.getPrevious();
                if (abstractInsnNode == null) {
                    continue;
                }
                switch (abstractInsnNode.getOpcode()) {
                    case ANEWARRAY:
                        tryOptimiseANewArray(insnList, abstractInsnNode);
                        break;
                    case INVOKESTATIC:
                        tryOptimiseInvokeStatic(className, methodNode, (MethodInsnNode) abstractInsnNode);
                        break;
                }
            }
        }

        return classNode;
    }

    private static void tryOptimiseANewArray(final InsnList insnList,final AbstractInsnNode abstractInsnNode) {
        AbstractInsnNode previous = abstractInsnNode.getPrevious();
        if (previous == null || previous.getOpcode() != ICONST_0) {
            return;
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
                return;
            }
        }
        insnList.insert(abstractInsnNode, new FieldInsnNode(GETSTATIC,
                "com/fox2code/foxloader/utils/EmptyArrays", fieldName,
                "[L" + ((TypeInsnNode) abstractInsnNode).desc + ";"));
        insnList.remove(abstractInsnNode);
        insnList.remove(previous);
    }

    private static void tryOptimiseInvokeStatic(String className, MethodNode methodNode, MethodInsnNode methodInsnNode) {
        if (methodInsnNode.name.equals("values") &&
                methodInsnNode.owner.startsWith("net/minecraft/") &&
                methodInsnNode.desc.equals("()[L" + methodInsnNode.owner + ";")) {
            AbstractInsnNode next = methodInsnNode.getNext();
            AbstractInsnNode next2 = next.getNext();
            AbstractInsnNode consuming = TransformerUtils.getConsumingInstruction(methodNode, next);
            AbstractInsnNode nextX;
            if (next2 == null || (next.getOpcode() != ARRAYLENGTH &&
                    // Always replace if it is always a get array index element.
                    !(consuming != null && consuming.getOpcode() == AALOAD) &&
                    // Try to detect the bytecode pattern of an array loop, not very precise.
                    !(next.getOpcode() == ASTORE && next2.getOpcode() == ALOAD &&
                            ((nextX = next2.getNext()) != null && nextX.getOpcode() == ARRAYLENGTH) &&
                            ((nextX = nextX.getNext()) != null && nextX.getOpcode() == ISTORE) &&
                            ((nextX = nextX.getNext()) != null && nextX.getOpcode() == ICONST_0) &&
                            ((nextX = nextX.getNext()) != null && nextX.getOpcode() == ISTORE)) &&
                    // Ignore self constructor, unless we know it's bad, not very precise.
                    !(next.getOpcode() != PUTSTATIC && next2.getOpcode() != AASTORE &&
                            methodNode.name.equals("<clinit>") && className.startsWith("net.minecraft.")))) {
                return;
            }
            methodInsnNode.name = "values$";
        }
    }
}
