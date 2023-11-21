package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class AsyncCapabilitiesTransformer implements PreClassTransformer {
    @Override
    public void transform(ClassNode classNode, String className) {
        if (className.equals("net.minecraft.src.server.ServerConfigurationManager") ||
                className.equals("net.minecraft.src.client.gui.GuiIngame")) {
            patchInit(classNode, "java/util/ArrayList", "com/fox2code/foxloader/launcher/utils/AsyncItrLinkedList");
        }
    }

    private static void patchInit(ClassNode classNode, String src, String dst) {
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.name.startsWith("<")) continue;
            InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode abstractInsnNode : insnList) {
                switch (abstractInsnNode.getOpcode()) {
                    case NEW: {
                        TypeInsnNode typeInsnNode = (TypeInsnNode) abstractInsnNode;
                        if (src.equals(typeInsnNode.desc))
                            typeInsnNode.desc = dst;
                        break;
                    }
                    case INVOKESPECIAL:
                    case INVOKEVIRTUAL: {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                        if (src.equals(methodInsnNode.owner))
                            methodInsnNode.owner = dst;
                    }
                }
            }
        }
    }
}
