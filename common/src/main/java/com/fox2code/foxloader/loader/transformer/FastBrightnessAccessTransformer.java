package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FastBrightnessAccessTransformer implements PreClassTransformer {
    private static final String CHUNK_CACHE = "net/minecraft/src/game/level/chunk/ChunkCache";
    private static final String IBLOCK_ACCESS = "net/minecraft/src/game/level/IBlockAccess";
    private static final String WORLD = "net/minecraft/src/game/level/World";
    private static final String BLOCK = "net/minecraft/src/game/block/Block";
    private static final List<String> optMethodExternal = Arrays.asList("getBrightness", "getLightBrightness");
    private static final List<String> optMethodInternal = Arrays.asList("getBrightness", "getLightBrightness",
            "getBlockLightValue", "getBlockLightValue_do", "getLightValue", "getLightValueExt");
    private static final String suffix = "WithId"; // so "getBrightness" -> "getBrightnessWithId"

    @Override
    public void transform(ClassNode classNode, String className) {
        switch (classNode.name) {
            case WORLD:
            case CHUNK_CACHE:
                patchBlockAccessImplementation(classNode);
                break;
            case IBLOCK_ACCESS:
                patchBlockAccessInterface(classNode);
                break;
            case BLOCK:
                patchBlock(classNode);
                break;
        }
    }

    private static MethodNode copyWithPrefix(String className, MethodNode methodNode, boolean stub) {
        MethodNode withId = TransformerUtils.copyMethodNode(methodNode);
        final String origName = methodNode.name;
        withId.access &=~ ACC_ABSTRACT | ACC_INTERFACE;
        withId.name += suffix;
        String origDesc = methodNode.desc;
        Type[] types = Type.getArgumentTypes(origDesc);
        int end = origDesc.indexOf(')');
        withId.desc = origDesc.substring(0, end) + "I" + origDesc.substring(end);
        withId.signature = null;
        final InsnList insnList = withId.instructions;
        if (stub) {
            withId.access &=~ ACC_ABSTRACT;
            insnList.clear();
            insnList.add(new VarInsnNode(ALOAD, 0));
            for (int i = 0;i < types.length; i++) {
                insnList.add(new VarInsnNode(
                        types[i].getOpcode(ILOAD), i + 1));
            }
            insnList.add(new MethodInsnNode(INVOKEINTERFACE,
                    className, methodNode.name, methodNode.desc, true));
            insnList.add(new InsnNode(
                    Type.getMethodType(methodNode.desc).getReturnType().getOpcode(IRETURN)));
            return withId;
        }

        // 0 this 1 arg + 1
        int arg = types.length + 1;
        withId.maxLocals++;

        for (LocalVariableNode localVariableNode : methodNode.localVariables) {
            if (localVariableNode.index >= arg) localVariableNode.index++;
        }

        for (AbstractInsnNode abstractInsnNode : insnList) {
            if (abstractInsnNode instanceof VarInsnNode && ((VarInsnNode) abstractInsnNode).var >= arg) {
                ((VarInsnNode) abstractInsnNode).var++;
            } else if (abstractInsnNode.getOpcode() == INVOKEVIRTUAL) {
                //noinspection DataFlowIssue (If it's INVOKEVIRTUAL we know it's a MethodInsnNode)
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                if (methodInsnNode.owner.equals(className)) {
                    if ("getBlockId".equals(methodInsnNode.name)) {
                        insnList.insert(methodInsnNode, new VarInsnNode(ILOAD, arg));
                        insnList.remove(methodInsnNode.getPrevious());
                        insnList.remove(methodInsnNode.getPrevious());
                        insnList.remove(methodInsnNode.getPrevious());
                        insnList.remove(methodInsnNode.getPrevious());
                        insnList.remove(methodInsnNode);
                    } else if (optMethodInternal.contains(methodInsnNode.name) &&
                            !origName.equals(methodInsnNode.name)) {
                        origDesc = methodInsnNode.desc;
                        end = origDesc.indexOf(')');
                        methodInsnNode.desc = origDesc.substring(0, end) + "I" + origDesc.substring(end);
                        methodInsnNode.name += suffix;
                        insnList.insertBefore(
                                methodInsnNode, new VarInsnNode(ILOAD, arg));
                    }
                }
            }
        }

        return withId;
    }

    private void patchBlockAccessInterface(ClassNode classNode) {
        if (classNode.version < V1_8) classNode.version = V1_8;
        LinkedList<MethodNode> stubs = new LinkedList<>();
        for (MethodNode methodNode : classNode.methods) {
            if (optMethodExternal.contains(methodNode.name)) {
                stubs.add(copyWithPrefix(classNode.name, methodNode, true));
            }
        }
        classNode.methods.addAll(stubs);
    }

    private void patchBlockAccessImplementation(ClassNode classNode) {
        LinkedList<MethodNode> stubs = new LinkedList<>();
        for (MethodNode methodNode : classNode.methods) {
            if (optMethodInternal.contains(methodNode.name)) {
                stubs.add(copyWithPrefix(classNode.name, methodNode, false));
            }
        }
        classNode.methods.addAll(stubs);
    }

    private void patchBlock(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            final InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode abstractInsnNode : insnList) {
                if (abstractInsnNode.getOpcode() == INVOKEINTERFACE) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.owner.equals(IBLOCK_ACCESS)) {
                        if (optMethodInternal.contains(methodInsnNode.name)) {
                            String origDesc = methodInsnNode.desc;
                            int end = origDesc.indexOf(')');
                            methodInsnNode.desc = origDesc.substring(0, end) + "I" + origDesc.substring(end);
                            methodInsnNode.name += suffix;
                            insnList.insertBefore(
                                    methodInsnNode, new VarInsnNode(ALOAD, 0));
                            insnList.insertBefore(
                                    methodInsnNode, new FieldInsnNode(GETFIELD, classNode.name, "blockID", "I"));
                        }
                    }
                }
            }
        }
    }
}
