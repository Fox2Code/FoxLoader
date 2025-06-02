package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class EntitiesPatch extends GamePatch {
    private static final String NetClientHandler = "net/minecraft/client/networking/NetClientHandler";
    private static final String EntityList = "net/minecraft/common/entity/EntityList";
    private static final String Entity = "net/minecraft/common/entity/Entity";
    private static final String World = "net/minecraft/common/world/World";
    private static final String EntityRendererManager = "net/minecraft/client/renderer/entity/EntityRendererManager";
    private static final String EntityRegistry = "com/fox2code/foxloader/registry/EntityRegistry";

    EntitiesPatch() {
        super(new String[]{NetClientHandler, EntityList, EntityRendererManager});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case NetClientHandler: {
                patchNetClientHandler(classNode);
                break;
            }
            case EntityList: {
                patchEntityList(classNode);
                break;
            }
            case EntityRendererManager: {
                TransformerUtils.makeFieldPublic(classNode, "entityRenderMap");
                break;
            }
        }
        return classNode;
    }

    private static void patchNetClientHandler(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == INVOKESTATIC) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (EntityList.equals(methodInsnNode.owner) &&
                            "createEntity".equals(methodInsnNode.name)) {
                        methodInsnNode.owner = EntityRegistry;
                        methodInsnNode.name = "createEntityRemote";
                    }
                }
            }
        }
    }

    private static void patchEntityList(ClassNode classNode) {
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "addMapping");
        methodNode.access &= ~(ACC_PRIVATE | ACC_PROTECTED);
        methodNode.access |= ACC_PUBLIC;
    }
}
