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
package com.fox2code.foxloader.patching.game;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Theses are just hotfixes for the game, dedicated to fixing ReIndev bugs and crashes.
 */
final class HotfixesPatch extends GamePatch {
    private static final String PropertyManager = "net/minecraft/server/util/PropertyManager";
    private static final String Properties = "java/util/Properties";
    private static final String InternalHotfixesHooks = "com/fox2code/foxloader/internal/InternalHotfixesHooks";
    static final boolean USE_HOTFIXES = true;

    HotfixesPatch() {
        super(new String[]{PropertyManager});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (PropertyManager.equals(classNode.name)) {
            patchPropertyManager(classNode);
        }
        return classNode;
    }

    private static void patchPropertyManager(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (Properties.equals(methodInsnNode.owner)) {
                        if ("load".equals(methodInsnNode.name) &&
                                "(Ljava/io/InputStream;)V".equals(methodInsnNode.desc)) {
                            methodNode.instructions.set(methodInsnNode, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC, InternalHotfixesHooks, "loadProperties",
                                    "(L" + Properties + ";Ljava/io/InputStream;)V"));
                        } else if ("store".equals(methodInsnNode.name) &&
                                "(Ljava/io/OutputStream;Ljava/lang/String;)V".equals(methodInsnNode.desc)) {
                            methodNode.instructions.set(methodInsnNode, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC, InternalHotfixesHooks, "storeProperties",
                                    "(L" + Properties + ";Ljava/io/OutputStream;Ljava/lang/String;)V"));
                        }
                    }
                }
            }
        }
    }
}
