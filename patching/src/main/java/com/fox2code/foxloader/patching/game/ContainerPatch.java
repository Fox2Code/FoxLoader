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
import org.objectweb.asm.tree.*;

final class ContainerPatch extends GamePatch {
    private static final String Container = "net/minecraft/common/block/container/Container";
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String EntityPlayerMP = "net/minecraft/server/entity/player/EntityPlayerMP";
    private static final String ContainerManager = "com/fox2code/foxloader/container/ContainerManager";

    ContainerPatch() {
        super(new String[]{Container, EntityPlayer, EntityPlayerMP});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case Container: {
                patchContainer(classNode);
                break;
            }
            case EntityPlayer: {
                patchEntityPlayer(classNode);
                break;
            }
            case EntityPlayerMP: {
                patchEntityPlayerMP(classNode);
                break;
            }
        }
        return classNode;
    }

    private void patchContainer(ClassNode classNode) {
        FieldNode fieldNode = TransformerUtils.getField(classNode, "playersList");
        MethodNode methodNode = new MethodNode(ACC_PUBLIC | ACC_FINAL,
                "getActiveViewers", "()" + fieldNode.desc, "()" + fieldNode.signature, null);
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        methodNode.instructions.add(new FieldInsnNode(GETFIELD,
                classNode.name, fieldNode.name, fieldNode.desc));
        methodNode.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(methodNode);
    }

    private static void patchEntityPlayer(ClassNode classNode) {
        MethodNode openFoxLoaderContainer = new MethodNode(ACC_PUBLIC,
                "openFoxLoaderContainer", "(L" + Container + ";III)Z", null, null);
        openFoxLoaderContainer.instructions.add(new VarInsnNode(ALOAD, 0));
        openFoxLoaderContainer.instructions.add(new VarInsnNode(ALOAD, 1));
        openFoxLoaderContainer.instructions.add(new VarInsnNode(ILOAD, 2));
        openFoxLoaderContainer.instructions.add(new VarInsnNode(ILOAD, 3));
        openFoxLoaderContainer.instructions.add(new VarInsnNode(ILOAD, 4));
        openFoxLoaderContainer.instructions.add(new MethodInsnNode(INVOKESTATIC,
                ContainerManager, "openFoxLoaderContainer",
                "(L"+ EntityPlayer + ";L" + Container + ";III)Z"));
        openFoxLoaderContainer.instructions.add(new InsnNode(IRETURN));
        // For debugging purposes
        TransformerUtils.setThisParameterName(classNode, openFoxLoaderContainer);
        TransformerUtils.setParameterName(openFoxLoaderContainer, 1, "container");
        TransformerUtils.setParameterName(openFoxLoaderContainer, 2, "x");
        TransformerUtils.setParameterName(openFoxLoaderContainer, 3, "y");
        TransformerUtils.setParameterName(openFoxLoaderContainer, 4, "z");
        classNode.methods.add(openFoxLoaderContainer);
    }

    private static void patchEntityPlayerMP(ClassNode classNode) {
        MethodNode getNextWindowId = TransformerUtils.getMethod(classNode, "getNextWindowId");
        getNextWindowId.access &= ~(ACC_PRIVATE | ACC_PROTECTED);
        getNextWindowId.access |= ACC_PUBLIC;
        TransformerUtils.makeFieldPublic(classNode, "currentWindowId");
    }
}
