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
import org.objectweb.asm.tree.*;

final class TexturesPatch extends GamePatch {
    private static final String TextureMap = "net/minecraft/client/renderer/block/TextureMap";

    TexturesPatch() {
        super(TextureMap);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (classNode.name.equals(TextureMap)) {
            return transformTextureMap(classNode);
        }
        return classNode;
    }

    private static ClassNode transformTextureMap(ClassNode classNode) {
        MethodNode basePath = new MethodNode(ACC_PRIVATE, "basePath", "(L" + String + ";)L" + String + ";", null, null);
        InsnList basePathInsns = basePath.instructions;
        LabelNode beginning = new LabelNode();
        basePathInsns.add(beginning);
        basePathInsns.add(new VarInsnNode(ALOAD, 1));
        basePathInsns.add(TransformerUtils.getNumberInsn('.'));
        basePathInsns.add(new MethodInsnNode(INVOKEVIRTUAL, String, "indexOf", "(I)I"));
        basePathInsns.add(new VarInsnNode(ISTORE, 2));
        LabelNode modIndexStart = new LabelNode();
        basePathInsns.add(modIndexStart);
        basePathInsns.add(new VarInsnNode(ILOAD, 2));
        basePathInsns.add(TransformerUtils.getNumberInsn(-1));
        LabelNode regularPath = new LabelNode();
        basePathInsns.add(new JumpInsnNode(IF_ICMPEQ, regularPath));
        // FoxLoader path
        // TransformerUtils.appendDebugMarker(basePathInsns);
        TransformerUtils.newStringBuilder(basePathInsns);
        basePathInsns.add(new LdcInsnNode("assets/"));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new VarInsnNode(ALOAD, 1));
        basePathInsns.add(TransformerUtils.getNumberInsn(0));
        basePathInsns.add(new VarInsnNode(ILOAD, 2));
        basePathInsns.add(new MethodInsnNode(INVOKEVIRTUAL, String,
                "substring", "(II)Ljava/lang/String;", false));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new LdcInsnNode("/"));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new VarInsnNode(ALOAD, 0));
        basePathInsns.add(new FieldInsnNode(GETFIELD, TextureMap, "basePath", "Ljava/lang/String;"));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new VarInsnNode(ALOAD, 1));
        basePathInsns.add(new VarInsnNode(ILOAD, 2));
        basePathInsns.add(TransformerUtils.getNumberInsn(1));
        basePathInsns.add(new InsnNode(Opcodes.IADD));
        basePathInsns.add(new MethodInsnNode(INVOKEVIRTUAL, String,
                "substring", "(I)Ljava/lang/String;", false));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new MethodInsnNode(INVOKEVIRTUAL, StringBuilder,
                "toString", "()Ljava/lang/String;", false));
        basePathInsns.add(new InsnNode(Opcodes.ARETURN));
        // Regular path
        basePathInsns.add(regularPath);
        TransformerUtils.newStringBuilder(basePathInsns);
        basePathInsns.add(new VarInsnNode(ALOAD, 0));
        basePathInsns.add(new FieldInsnNode(GETFIELD, TextureMap, "basePath", "Ljava/lang/String;"));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new VarInsnNode(ALOAD, 1));
        TransformerUtils.appendStringBuilder(basePathInsns);
        basePathInsns.add(new MethodInsnNode(INVOKEVIRTUAL, StringBuilder,
                "toString", "()Ljava/lang/String;", false));
        basePathInsns.add(new InsnNode(Opcodes.ARETURN));
        LabelNode end = new LabelNode();
        basePathInsns.add(end);
        basePath.localVariables.add(new LocalVariableNode("this", "L" + TextureMap + ";", null, beginning, end, 0));
        basePath.localVariables.add(new LocalVariableNode("textureName", "L" + String + ";", null, beginning, end, 1));
        basePath.localVariables.add(new LocalVariableNode("modIndex", "I", null, modIndexStart, end, 2));
        basePath.maxLocals = 3;
        classNode.methods.add(basePath);
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("basePath") ||
                    methodNode.name.startsWith("<") ||
                    (methodNode.access & ACC_STATIC) != 0) {
                continue;
            }
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
                if (abstractInsnNode.getOpcode() == GETFIELD) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    if (fieldInsnNode.owner.equals(TextureMap) && fieldInsnNode.name.equals("basePath")) {
                        patchBasePathUsage(methodNode, fieldInsnNode);
                    }
                }
            }
        }

        return classNode;
    }

    private static void patchBasePathUsage(MethodNode methodNode, FieldInsnNode basePath) {
        final AbstractInsnNode appendToRemove = TransformerUtils.nextCodeInsn(basePath);
        if (appendToRemove.getOpcode() != Opcodes.INVOKEVIRTUAL)
            throw new IllegalStateException("Unsupported code?");
        AbstractInsnNode nextAppend = TransformerUtils.nextCodeInsn(appendToRemove);
        while (nextAppend.getOpcode() != Opcodes.INVOKEVIRTUAL ||
                !((MethodInsnNode) nextAppend).name.equals("append")) {
            nextAppend = TransformerUtils.nextCodeInsn(nextAppend);
        }
        methodNode.instructions.insertBefore(nextAppend, new MethodInsnNode(
                INVOKEVIRTUAL, TextureMap, "basePath", "(L" + String + ";)L" + String + ";"));
        methodNode.instructions.remove(basePath);
        methodNode.instructions.remove(appendToRemove);
    }
}
