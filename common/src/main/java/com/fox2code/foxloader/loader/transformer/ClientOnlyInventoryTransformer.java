package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class ClientOnlyInventoryTransformer implements PreClassTransformer {
    private static final String PLAYER_CONTROLLER_MP = "net.minecraft.src.client.player.PlayerControllerMP";
    private static final String CLIENT_NETWORK_HANDLER = "net.minecraft.src.client.packets.NetClientHandler";
    private static final String GUI_CONTAINER_CREATIVE = "net.minecraft.src.client.gui.GuiContainerCreative";
    private static final String GUI_CONTAINER = "net.minecraft.src.client.gui.GuiContainer";

    private static final String ASM_INVENTORY_BASIC =
            "net/minecraft/src/client/inventory/InventoryBasic";
    private static final String ASM_INVENTORY_BASIC_CLIENT_ONLY =
            "com/fox2code/foxloader/client/gui/InventoryBasicClientOnly";

    @Override
    public void transform(ClassNode classNode, String className) {
        switch (className) {
            case PLAYER_CONTROLLER_MP:
                patchPlayerControllerMP(classNode);
                return;
            case CLIENT_NETWORK_HANDLER:
                patchNetClientHandler(classNode);
                return;
            case GUI_CONTAINER:
                patchGuiContainer(classNode);
                return;
            case GUI_CONTAINER_CREATIVE:
                patchGuiContainerCreative(classNode);
        }
    }

    private void patchPlayerControllerMP(ClassNode classNode) {
        MethodNode sendClickSlot = TransformerUtils
                .getMethod(classNode, "clickSlot");
        InsnList earlyInject = new InsnList();
        LabelNode labelNode = new LabelNode();
        earlyInject.add(new VarInsnNode(ILOAD, 1));
        earlyInject.add(new InsnNode(ICONST_M1));
        earlyInject.add(new JumpInsnNode(IF_ICMPNE, labelNode));
        earlyInject.add(new VarInsnNode(ALOAD, 0));
        earlyInject.add(new VarInsnNode(ILOAD, 1));
        earlyInject.add(new VarInsnNode(ILOAD, 2));
        earlyInject.add(new VarInsnNode(ILOAD, 3));
        earlyInject.add(new VarInsnNode(ILOAD, 4));
        earlyInject.add(new VarInsnNode(ALOAD, 5));
        earlyInject.add(new MethodInsnNode(INVOKESPECIAL,
                "net/minecraft/src/client/player/PlayerController", "clickSlot",
                "(IIIILnet/minecraft/src/game/entity/player/EntityPlayer;)Lnet/minecraft/src/game/item/ItemStack;"));
        earlyInject.add(new InsnNode(ARETURN));
        earlyInject.add(labelNode);
        AbstractInsnNode firstCodeNode =
                sendClickSlot.instructions.getFirst();
        while (firstCodeNode.getOpcode() == -1)
            firstCodeNode = firstCodeNode.getNext();
        sendClickSlot.instructions.insertBefore(firstCodeNode, earlyInject);
    }

    private void patchNetClientHandler(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode abstractInsnNode : insnList) {
                if (abstractInsnNode.getOpcode() == GETFIELD &&
                        ((FieldInsnNode)abstractInsnNode).name.equals("currentContainer")) {
                    insnList.insertBefore(abstractInsnNode, new MethodInsnNode(INVOKESTATIC, classNode.name, "getNetworkContainer",
                            "(Lnet/minecraft/src/game/entity/player/EntityPlayer;)Lnet/minecraft/src/client/gui/Container;", false));
                    insnList.remove(abstractInsnNode);
                }
            }
        }


        final MethodNode getNetworkContainer = new MethodNode(ACC_PUBLIC | ACC_STATIC, "getNetworkContainer",
                "(Lnet/minecraft/src/game/entity/player/EntityPlayer;)Lnet/minecraft/src/client/gui/Container;",
                null, null);
        getNetworkContainer.instructions.add(new VarInsnNode(ALOAD, 0));
        getNetworkContainer.instructions.add(new FieldInsnNode(GETFIELD,
                "net/minecraft/src/game/entity/player/EntityPlayer",
                "currentContainer", "Lnet/minecraft/src/client/gui/Container;"));
        getNetworkContainer.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "com/fox2code/foxloader/client/gui/ContainerWrapped", "getNetworkContainer",
                "(Lnet/minecraft/src/client/gui/Container;)Lnet/minecraft/src/client/gui/Container;", true));
        getNetworkContainer.instructions.add(new VarInsnNode(ASTORE, 1));
        getNetworkContainer.instructions.add(new VarInsnNode(ALOAD, 1));
        LabelNode noFallback = new LabelNode();
        getNetworkContainer.instructions.add(new JumpInsnNode(IFNONNULL, noFallback));
        getNetworkContainer.instructions.add(new VarInsnNode(ALOAD, 0));
        getNetworkContainer.instructions.add(new FieldInsnNode(GETFIELD,
                "net/minecraft/src/game/entity/player/EntityPlayer",
                "playerContainer", "Lnet/minecraft/src/client/gui/Container;"));
        getNetworkContainer.instructions.add(new VarInsnNode(ASTORE, 1));
        getNetworkContainer.instructions.add(noFallback);
        getNetworkContainer.instructions.add(new VarInsnNode(ALOAD, 1));
        getNetworkContainer.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getNetworkContainer);
    }

    private void patchGuiContainer(ClassNode classNode) {
        MethodNode sendClickSlot = TransformerUtils
                .getMethod(classNode, "sendClickSlot");
        sendClickSlot.access = ACC_PUBLIC;
        InsnList instructions = sendClickSlot.instructions;
        FieldInsnNode windowId;
        for (AbstractInsnNode abstractInsnNode : instructions) {
            if (abstractInsnNode.getOpcode() == GETFIELD &&
                    (windowId = (FieldInsnNode) abstractInsnNode)
                            .name.equals("windowId")) {
                InsnList instructionsBuffer = new InsnList();
                LabelNode notClientOnly = new LabelNode();
                instructionsBuffer.add(new VarInsnNode(ALOAD, 1));
                instructionsBuffer.add(new JumpInsnNode(IFNULL, notClientOnly));
                instructionsBuffer.add(new VarInsnNode(ALOAD, 1));
                instructionsBuffer.add(new FieldInsnNode(GETFIELD,
                        "net/minecraft/src/client/gui/Slot", "inventory",
                        "Lnet/minecraft/src/client/inventory/IInventory;"));
                instructionsBuffer.add(new TypeInsnNode(INSTANCEOF,
                        "com/fox2code/foxloader/client/gui/InventoryClientOnly"));
                instructionsBuffer.add(new JumpInsnNode(IFEQ, notClientOnly));
                instructionsBuffer.add(new InsnNode(ICONST_M1));
                LabelNode clientOnly = new LabelNode();
                instructionsBuffer.add(new JumpInsnNode(GOTO, clientOnly));
                instructionsBuffer.add(notClientOnly);
                instructions.insertBefore(windowId.getPrevious()
                        .getPrevious(), instructionsBuffer);
                instructions.insert(windowId, clientOnly);
                return;
            }
        }
    }

    private void patchGuiContainerCreative(ClassNode classNode) {
        TransformerUtils.getMethod(classNode, "sendClickSlot").access = ACC_PUBLIC;
        MethodNode clinit = TransformerUtils.getMethod(classNode, "<clinit>");
        for (AbstractInsnNode abstractInsnNode : clinit.instructions) {
            if (abstractInsnNode.getOpcode() == NEW) {
                TypeInsnNode typeInsnNode = (TypeInsnNode) abstractInsnNode;
                if (ASM_INVENTORY_BASIC.equals(typeInsnNode.desc))
                    typeInsnNode.desc = ASM_INVENTORY_BASIC_CLIENT_ONLY;
            } else if (abstractInsnNode.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                if (ASM_INVENTORY_BASIC.equals(methodInsnNode.owner))
                    methodInsnNode.owner = ASM_INVENTORY_BASIC_CLIENT_ONLY;
            }
        }
    }
}
