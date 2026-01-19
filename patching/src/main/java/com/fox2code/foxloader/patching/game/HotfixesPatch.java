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
import org.objectweb.asm.tree.*;

import java.util.Objects;

/**
 * Theses are just hotfixes for the game, dedicated to fixing ReIndev bugs and crashes.
 */
final class HotfixesPatch extends GamePatch {
    private static final String InternalHotfixesHooks = "com/fox2code/foxloader/internal/InternalHotfixesHooks";
    private static final String Packet250PluginMessage = "net/minecraft/common/networking/Packet250PluginMessage";
    static final boolean USE_HOTFIXES = true;

    HotfixesPatch() {
        super(new String[]{Packet250PluginMessage});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (Packet250PluginMessage.equals(classNode.name)) {
            hotfixPacket250PluginMessage(classNode);
        }
        return classNode;
    }

    private static void hotfixPacket250PluginMessage(ClassNode classNode) {
        MethodNode readPacketData = TransformerUtils.getMethod(classNode, "readPacketData");
        AbstractInsnNode dup = null, if_lt = null;
        MethodInsnNode readInstruction = null;
        for (AbstractInsnNode abstractInsnNode : readPacketData.instructions) {
            if (abstractInsnNode.getOpcode() == DUP && dup == null) {
                dup = abstractInsnNode;
            }
            if (abstractInsnNode.getOpcode() == IFLT && dup != null && if_lt == null) {
                if_lt = abstractInsnNode;
            }
            if (abstractInsnNode.getOpcode() == INVOKEVIRTUAL) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                if (methodInsnNode.name.equals("read")) {
                    readInstruction = methodInsnNode;
                    break;
                } else if (methodInsnNode.name.equals("readFully")) {
                    return;
                }
            }
        }
        Objects.requireNonNull(dup, "dup");
        Objects.requireNonNull(if_lt, "if_lt");
        Objects.requireNonNull(readInstruction, "readInstruction");
        readPacketData.instructions.remove(dup);
        readPacketData.instructions.remove(if_lt);
        readInstruction.name = "readFully";
        readInstruction.desc = "([B)V";
        TransformerUtils.removeInstructionsInRange(readPacketData.instructions,
                readInstruction, TransformerUtils.previousCodeInsn(
                        TransformerUtils.getEndingLabelNode(readPacketData), 2).getNext());
    }
}
