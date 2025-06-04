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

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Theses are just hotfixes for the game, dedicated to fixing ReIndev bugs and crashes.
 */
final class HotfixesPatch extends GamePatch {
    private static final String Packet =  "net/minecraft/common/networking/Packet";
    private static final String Packet249Chunk =  "net/minecraft/common/networking/Packet249Chunk";
    private static final String Minecraft =  "net/minecraft/client/Minecraft";
    private static final String GameSettings =  "net/minecraft/client/util/GameSettings";
    static final boolean USE_HOTFIXES = true;

    HotfixesPatch() {
        super(new String[]{Packet, Minecraft, GameSettings});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case Packet:
                patchPacket(classNode);
                break;
            case Minecraft:
                patchMinecraft(classNode);
                break;
            case GameSettings:
                patchGameSettings(classNode);
                break;
        }
        return classNode;
    }

    private static void patchPacket(ClassNode classNode) {
        // Hotfix Packet249Chunk being parsed by the server if sent by the client.
        MethodNode clInit = TransformerUtils.getMethod(classNode, "<clinit>");
        for (AbstractInsnNode abstractInsnNode : clInit.instructions) {
            if (abstractInsnNode.getOpcode() == LDC) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                if (ldcInsnNode.cst instanceof Type &&
                        Packet249Chunk.equals(((Type) ldcInsnNode.cst).getInternalName())) {
                    AbstractInsnNode previous = ldcInsnNode.getPrevious();
                    AbstractInsnNode previous2 = previous.getPrevious();
                    if (previous.getOpcode() == ICONST_1 && previous2.getOpcode() == ICONST_1) {
                        clInit.instructions.set(previous, new InsnNode(ICONST_0));
                        clInit.instructions.set(previous2, new InsnNode(ICONST_0));
                    }
                    break;
                }
            }
        }
    }

    private static void patchMinecraft(ClassNode classNode) {
        // Hotfix switching to fullscreen sometimes causing the game to crash.
        MethodNode toggleFullscreenImpl = TransformerUtils.findMethod(classNode, "toggleFullscreenImpl");
        FieldNode gameSettingsField = TransformerUtils.findFieldDesc(classNode, "L" + GameSettings + ";");
        if (toggleFullscreenImpl == null || gameSettingsField == null) return;
        for (AbstractInsnNode abstractInsnNode : toggleFullscreenImpl.instructions) {
            MethodInsnNode methodInsnNode;
            if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL &&
                    GameSettings.equals((methodInsnNode = (MethodInsnNode) abstractInsnNode).owner) &&
                    "setOptionValue".equals(methodInsnNode.name) && methodInsnNode.desc.endsWith(")V")) {
                AbstractInsnNode start = TransformerUtils.previousNonCodeInsn(methodInsnNode);
                TransformerUtils.removeInstructionsInRange(
                        toggleFullscreenImpl.instructions, start, methodInsnNode.getNext());
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(ALOAD, 0));
                insnList.add(new FieldInsnNode(GETFIELD, Minecraft,
                        gameSettingsField.name, gameSettingsField.desc));
                insnList.add(new VarInsnNode(ILOAD, 1));
                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, GameSettings,
                        "setFullscreenState", "(Z)V"));
                toggleFullscreenImpl.instructions.insert(start, insnList);
            }
        }
    }

    private static void patchGameSettings(ClassNode classNode) {
        // Hotfix switching to fullscreen sometimes causing the game to crash.
        if (TransformerUtils.findMethod(classNode, "setFullscreenState", "(Z)V") != null) {
            return;
        }
        FieldNode fullscreenField = TransformerUtils.getField(classNode, "fullScreen");
        MethodNode setFullscreenState = new MethodNode(ACC_PUBLIC,
                "setFullscreenState", "(Z)V", null, null);
        setFullscreenState.instructions.add(new VarInsnNode(ALOAD, 0));
        setFullscreenState.instructions.add(new VarInsnNode(ALOAD, 1));
        setFullscreenState.instructions.add(new FieldInsnNode(PUTFIELD,
                GameSettings, fullscreenField.name, fullscreenField.desc));
        setFullscreenState.instructions.add(new InsnNode(RETURN));
        TransformerUtils.setThisParameterName(classNode, setFullscreenState);
        TransformerUtils.setParameterName(setFullscreenState, 1, "fullscreen");
        classNode.methods.add(setFullscreenState);
    }
}
