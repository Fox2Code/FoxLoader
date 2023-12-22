package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

/**
 * Hotfix for DeApplet bundling a World class from 2.8.1_04 that is incompatible with 2.8.1_05
 */
public class DeApplet281Hotfix implements PreClassTransformer {
    @Override
    public void transform(ClassNode classNode, String className) {
        if (!className.equals("net.minecraft.src.game.level.World")) return;
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
                if (abstractInsnNode.getOpcode() == INVOKESTATIC) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.name.equals("chunkXYZ2Int") &&
                            methodInsnNode.desc.equals("(III)Ljava/lang/Long;")) {
                        methodInsnNode.desc = "(III)J";
                        abstractInsnNode = methodInsnNode.getNext();
                        if (abstractInsnNode.getOpcode() != INVOKEVIRTUAL ||
                                !(methodInsnNode = ((MethodInsnNode)
                                        abstractInsnNode)).name.equals("longValue")) {
                            throw new RuntimeException("Unsupported and broken version of DeApplet");
                        }
                        methodNode.instructions.remove(methodInsnNode);
                    }
                } else if (abstractInsnNode.getOpcode() == GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    if (fieldInsnNode.owner.equals("net/minecraft/src/game/block/Block") &&
                            !fieldInsnNode.name.equals("lightRenderPass") &&
                            fieldInsnNode.desc.equals("[Z")) {
                        abstractInsnNode = fieldInsnNode.getNext().getNext();
                        if (abstractInsnNode.getOpcode() != BALOAD) {
                            abstractInsnNode = abstractInsnNode.getNext().getNext();
                            if (abstractInsnNode.getOpcode() != BALOAD) {
                                throw new RuntimeException("Unsupported and broken version of DeApplet");
                            }
                        }
                        fieldInsnNode.desc = "Ljava/util/BitSet;";
                        methodNode.instructions.insert(abstractInsnNode,
                                new MethodInsnNode(INVOKEVIRTUAL, "java/util/BitSet", "get", "(I)Z"));
                        methodNode.instructions.remove(abstractInsnNode);
                    }
                }
            }
        }
    }
}
