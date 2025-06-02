package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

final class ContainerPatch extends GamePatch {
    private static final String Container = "net/minecraft/common/block/container/Container";
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String EntityPlayerMP = "net/minecraft/server/entity/player/EntityPlayerMP";
    private static final String ContainerManager = "com/fox2code/foxloader/container/ContainerManager";

    ContainerPatch() {
        super(new String[]{EntityPlayer, EntityPlayerMP});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
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
