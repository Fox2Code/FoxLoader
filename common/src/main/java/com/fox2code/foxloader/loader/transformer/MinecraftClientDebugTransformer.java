package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class MinecraftClientDebugTransformer implements PreClassTransformer {
    @Override
    public void transform(ClassNode classNode, String className) {
        if ("net.minecraft.src.client.ThreadGetSkin".equals(className)) {
            MethodNode methodNode = TransformerUtils.getMethod(classNode, "run");
            if (methodNode.tryCatchBlocks == null) return;
            hotfixThreadGetSkin(methodNode);
            return;
        }
        if (!"net.minecraft.client.Minecraft".equals(className)) return;
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "run");
        if (methodNode.tryCatchBlocks == null) return;
        patchExceptionPrint(methodNode, "java/lang/OutOfMemoryError");
        patchExceptionPrint(methodNode, "net/minecraft/src/client/MinecraftException");
    }

    private void patchExceptionPrint(MethodNode methodNode, String type) {
        for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
            if (type.equals(tryCatchBlockNode.type)) {
                AbstractInsnNode abstractInsnNode = tryCatchBlockNode.handler;
                int opcode;
                while (abstractInsnNode != null &&
                        (opcode = abstractInsnNode.getOpcode()) != ASTORE && opcode != POP) {
                    abstractInsnNode = abstractInsnNode.getNext();
                }
                if (abstractInsnNode == null) return;
                opcode = abstractInsnNode.getOpcode();
                InsnList toInject = new InsnList();
                if (opcode == ASTORE) {
                    toInject.add(new VarInsnNode(ALOAD,
                            ((VarInsnNode) abstractInsnNode).var));
                }
                toInject.add(new MethodInsnNode(INVOKEVIRTUAL,
                        type, "printStackTrace", "()V", false));
                methodNode.instructions.insert(abstractInsnNode, toInject);
                if (opcode == POP) {
                    methodNode.instructions.remove(abstractInsnNode);
                }
                return;
            }
        }
    }

    private void hotfixThreadGetSkin(MethodNode methodNode) {
        for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
            if ("java/lang/Exception".equals(tryCatchBlockNode.type)) {
                tryCatchBlockNode.type = "java/lang/Throwable";
                AbstractInsnNode abstractInsnNode = tryCatchBlockNode.handler.getNext();
                if (abstractInsnNode instanceof FrameNode) {
                    FrameNode frameNode = (FrameNode) abstractInsnNode;
                    if (frameNode.stack.size() == 1 &&
                            "java/lang/Exception".equals(
                                    frameNode.stack.get(0))) {
                        frameNode.stack.set(0, "java/lang/Throwable");
                    }
                }
                return;
            }
        }
    }
}
