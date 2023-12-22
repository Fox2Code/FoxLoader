package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class MinecraftClientDebugTransformer implements PreClassTransformer {
    @Override
    public void transform(ClassNode classNode, String className) {
        switch (className) {
            case "net.minecraft.client.Minecraft": {
                MethodNode methodNode = TransformerUtils.getMethod(classNode, "run");
                if (methodNode.tryCatchBlocks == null) return;
                patchExceptionPrint(methodNode, "java/lang/OutOfMemoryError");
                patchExceptionPrint(methodNode, "net/minecraft/src/client/MinecraftException");
                return;
            }
            case "net.minecraft.src.client.ThreadGetSkin": {
                MethodNode methodNode = TransformerUtils.getMethod(classNode, "run");
                if (methodNode.tryCatchBlocks == null) return;
                patchExceptionType(methodNode, "java/lang/Exception", "java/lang/Throwable");
                return;
            }
            case "net.minecraft.src.client.renderer.block.TextureMap": {
                MethodNode methodNode = TransformerUtils.getMethod(classNode, "refreshTextures");
                if (methodNode.tryCatchBlocks == null) return;
                patchExceptionType(methodNode, "java/io/IOException", "java/lang/Exception");
                return;
            }
            default:
        }
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

    private void patchExceptionType(MethodNode methodNode, String from, String to) {
        for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
            if (from.equals(tryCatchBlockNode.type)) {
                tryCatchBlockNode.type = to;
                AbstractInsnNode abstractInsnNode = tryCatchBlockNode.handler.getNext();
                if (abstractInsnNode instanceof FrameNode) {
                    FrameNode frameNode = (FrameNode) abstractInsnNode;
                    if (frameNode.stack.size() == 1 &&
                            from.equals(
                                    frameNode.stack.get(0))) {
                        frameNode.stack.set(0, to);
                    }
                }
                return;
            }
        }
    }
}
