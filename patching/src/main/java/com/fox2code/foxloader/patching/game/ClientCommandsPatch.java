package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

final class ClientCommandsPatch extends GamePatch {
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String GuiCommandCompletion = "net/minecraft/client/gui/GuiCommandCompletion";
    private static final String CommandRegistry$Internal = "com/fox2code/foxloader/registry/CommandRegistry$Internal";
    private static final String ClientCommandCompletion = "net/minecraft/client/command/ClientCommandCompletion";

    ClientCommandsPatch() {
        super(new String[]{Minecraft, GuiCommandCompletion});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case Minecraft:
                patchMinecraft(classNode);
                break;
            case GuiCommandCompletion:
                patchGuiCommandCompletion(classNode);
                break;
        }
        return classNode;
    }

    private static void patchMinecraft(ClassNode classNode) {
        MethodNode sendAbritraryCommand = TransformerUtils.getMethod(classNode, "sendAbritraryCommand");
        InsnList prelude = new InsnList();
        prelude.add(new VarInsnNode(ALOAD, 1));
        prelude.add(new MethodInsnNode(INVOKESTATIC, CommandRegistry$Internal,
                "handleClientCommand", "(L" + String + ";)Z"));
        LabelNode notHandled = new LabelNode();
        prelude.add(new JumpInsnNode(IFEQ, notHandled));
        prelude.add(new InsnNode(RETURN));
        prelude.add(notHandled);
        TransformerUtils.insertToBeginningOfCode(sendAbritraryCommand, prelude);
    }

    private static void patchGuiCommandCompletion(ClassNode classNode) {
        MethodNode updateCompletions = TransformerUtils.getMethod(classNode, "updateCompletions");
        for (AbstractInsnNode abstractInsnNode : updateCompletions.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode methodNode = (MethodInsnNode) abstractInsnNode;
                if (ClientCommandCompletion.equals(methodNode.owner) &&
                        "complete".equals(methodNode.name)) {
                    updateCompletions.instructions.insert(methodNode,
                            new MethodInsnNode(INVOKESTATIC, CommandRegistry$Internal, "handleClientCompletions",
                            methodNode.desc.replace("(", "(L" + ClientCommandCompletion + ";")));
                    updateCompletions.instructions.remove(methodNode);
                    return;
                }
            }
        }
    }
}
