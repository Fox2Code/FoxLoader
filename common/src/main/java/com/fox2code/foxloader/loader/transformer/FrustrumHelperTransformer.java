package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class FrustrumHelperTransformer implements PreClassTransformer {
    private static final String PARTICLE = "net/minecraft/src/client/particle/EntityFX";
    private static final String BOUNDINGBOX ="net/minecraft/src/client/physics/AxisAlignedBB";
    private static final String FRUSTRUM = "net/minecraft/src/client/renderer/Frustrum";
    private static final String FRUSTRUM_HELPER = "com/fox2code/foxloader/client/FrustrumHelper";
    private static final String FRUSTRUM_HOOKS = "com/fox2code/foxloader/client/FrustrumHelper$Hooks";

    @Override
    public void transform(ClassNode classNode, String className) {
        if (!className.startsWith("net.minecraft.")) return;
        boolean entityRenderer = className.equals("net.minecraft.src.client.renderer.EntityRenderer");
        boolean effectRenderer = className.equals("net.minecraft.src.client.particle.EffectRenderer");
        for (MethodNode methodNode : classNode.methods) {
            InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == INVOKESPECIAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.owner.equals(FRUSTRUM) &&
                            methodInsnNode.name.equals("<init>")) {
                        AbstractInsnNode dup = methodInsnNode.getPrevious();
                        AbstractInsnNode anew = dup.getPrevious();
                        if (dup.getOpcode() == DUP && anew.getOpcode() == NEW) {
                            insnList.insert(methodInsnNode, new FieldInsnNode(
                                    GETSTATIC, FRUSTRUM_HELPER, "frustrum", "L" + FRUSTRUM + ";"));
                            insnList.remove(methodInsnNode);
                            insnList.remove(dup);
                            insnList.remove(anew);
                            System.out.println("Optimized frustrum: " + className);
                        }
                    }
                } else if (entityRenderer && abstractInsnNode.getOpcode() == INVOKEVIRTUAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.owner.equals(FRUSTRUM) &&
                            methodInsnNode.name.equals("setPosition")) {
                        insnList.insertBefore(methodInsnNode, new MethodInsnNode(INVOKESTATIC,
                                FRUSTRUM_HOOKS, "update", "(L" + FRUSTRUM + ";DDD)V", false));
                        insnList.remove(methodInsnNode);
                    }
                } else if (effectRenderer && abstractInsnNode.getOpcode() == CHECKCAST &&
                        methodNode.name.startsWith("render")) {
                    TypeInsnNode typeInsnNode = (TypeInsnNode) abstractInsnNode;
                    if (!typeInsnNode.desc.equals(PARTICLE)) continue;
                    abstractInsnNode = typeInsnNode.getNext();
                    if (!(abstractInsnNode.getOpcode() == ASTORE)) continue;
                    VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                    InsnList insns = new InsnList();
                    LabelNode endLabel = new LabelNode();
                    insns.add(new VarInsnNode(ALOAD, varInsnNode.var));
                    insns.add(new FieldInsnNode(GETFIELD, PARTICLE,
                            "boundingBox", "L" + BOUNDINGBOX + ";"));
                    insns.add(new MethodInsnNode(INVOKESTATIC, FRUSTRUM_HELPER,
                            "isBoundingBoxInFrustum", "(L" + BOUNDINGBOX + ";)Z"));
                    insns.add(new JumpInsnNode(IFEQ, endLabel));
                    AbstractInsnNode nextReal = varInsnNode.getNext();
                    insnList.insert(varInsnNode, insns);
                    while (nextReal != null && nextReal.getOpcode() != INVOKEVIRTUAL) {
                        nextReal = nextReal.getNext();
                    }
                    insnList.insert(nextReal, endLabel);
                }
            }
        }
    }
}
