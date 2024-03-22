package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class NetworkMappingTransformer implements PreClassTransformer {
    private static final String CLIENT_NETWORK_MANAGER = "net.minecraft.src.client.packets.NetworkManager";
    private static final String SERVER_NETWORK_MANAGER = "net.minecraft.src.server.packets.NetworkManager";

    @Override
    public void transform(ClassNode classNode, String className) {
        if (className.equals(CLIENT_NETWORK_MANAGER) ||
                className.equals(SERVER_NETWORK_MANAGER)) {
            for (MethodNode methodNode : classNode.methods) {
                for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                    switch (abstractInsnNode.getOpcode()) {
                        case NEW: {
                            TypeInsnNode insn = (TypeInsnNode) abstractInsnNode;
                            insn.desc = mapAsmName(insn.desc);
                            break;
                        }
                        case INVOKESPECIAL: {
                            MethodInsnNode insn = (MethodInsnNode) abstractInsnNode;
                            insn.owner = mapAsmName(insn.owner);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static String mapAsmName(String asmName) {
        switch (asmName) {
            default:
                return asmName;
            case "java/io/DataInputStream":
                return "com/fox2code/foxloader/network/io/NetworkDataInputStream";
            case "java/io/DataOutputStream":
                return "com/fox2code/foxloader/network/io/NetworkDataOutputStream";
        }
    }
}
