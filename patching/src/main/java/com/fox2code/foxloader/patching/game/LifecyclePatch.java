package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

final class LifecyclePatch extends GamePatch {
    private static final String World = "net/minecraft/common/world/World";
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String MinecraftServer = "net/minecraft/server/MinecraftServer";
    private static final String ConnectionType = "com/fox2code/foxloader/network/ConnectionType";
    private static final String LifecycleStartEvent = "com/fox2code/foxloader/event/lifecycle/LifecycleStartEvent";
    private static final String LifecycleStopEvent = "com/fox2code/foxloader/event/lifecycle/LifecycleStopEvent";

    LifecyclePatch() {
        super(new String[]{});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case Minecraft:
                patchMinecraft(classNode);
                break;
            case MinecraftServer:
                patchMinecraftServer(classNode);
                break;
        }
        return classNode;
    }

    private static void patchMinecraft(ClassNode classNode) {
        MethodNode changeWorld = TransformerUtils.getMethod(classNode, "changeWorld",
                "(L" + World + ";Ljava/lang/String;Lnet/minecraft/common/entity/player/EntityPlayer;)V");
        FieldNode worldField = TransformerUtils.findField(classNode, "theWorld");
        if (worldField == null) {
            worldField = TransformerUtils.getFieldDesc(classNode, "L" + World + ";");
        }
        // Handle client lifecycle stops.
        InsnList prelude = new InsnList();
        LabelNode endOfPrelude = new LabelNode();
        LabelNode mayStartingUp = new LabelNode();
        LabelNode start = new LabelNode();
        prelude.add(start);
        LocalVariableNode flIsStartingUp =
                TransformerUtils.injectLocalVariable(changeWorld, "flIsStartingUp", "Z", start);
        prelude.add(new InsnNode(ICONST_0));
        prelude.add(new VarInsnNode(ISTORE, flIsStartingUp.index));
        prelude.add(new VarInsnNode(ALOAD, 1));
        prelude.add(new JumpInsnNode(IFNONNULL, mayStartingUp));
        prelude.add(new VarInsnNode(ALOAD, 0));
        prelude.add(new FieldInsnNode(GETFIELD, Minecraft, worldField.name, worldField.desc));
        prelude.add(new JumpInsnNode(IFNULL, endOfPrelude));
        prelude.add(new VarInsnNode(ALOAD, 0));
        prelude.add(new FieldInsnNode(GETFIELD, Minecraft, worldField.name, worldField.desc));
        prelude.add(new FieldInsnNode(GETFIELD, World, "isRemote", "Z"));
        prelude.add(generateClientInstructions(LifecycleStopEvent, endOfPrelude));
        prelude.add(new JumpInsnNode(GOTO, endOfPrelude));
        prelude.add(mayStartingUp);
        prelude.add(new VarInsnNode(ALOAD, 0));
        prelude.add(new FieldInsnNode(GETFIELD, Minecraft, worldField.name, worldField.desc));
        prelude.add(new JumpInsnNode(IFNONNULL, endOfPrelude));
        prelude.add(new InsnNode(ICONST_1));
        prelude.add(new VarInsnNode(ISTORE, flIsStartingUp.index));
        prelude.add(endOfPrelude);
        TransformerUtils.insertToBeginningOfCode(changeWorld, prelude);
        // Handle client lifecycle starts.
        InsnList end = new InsnList();
        LabelNode notStartingUp = new LabelNode();
        end.add(new VarInsnNode(ILOAD, flIsStartingUp.index));
        end.add(new JumpInsnNode(IFEQ, notStartingUp));
        end.add(new VarInsnNode(ALOAD, 0));
        end.add(new FieldInsnNode(GETFIELD, Minecraft, worldField.name, worldField.desc));
        end.add(new FieldInsnNode(GETFIELD, World, "isRemote", "Z"));
        end.add(generateClientInstructions(LifecycleStartEvent, notStartingUp));
        end.add(notStartingUp);
        TransformerUtils.insertToEndOfCode(changeWorld, end);
    }

    private static void patchMinecraftServer(ClassNode classNode) {
        MethodNode startServer = TransformerUtils.getMethod(classNode, "startServer");
        TransformerUtils.insertToEndOfCode(startServer, generateInstructions(LifecycleStartEvent, "SERVER_ONLY"));
        MethodNode stopServer = TransformerUtils.getMethod(classNode, "stopServer");
        TransformerUtils.insertToBeginningOfCode(stopServer, generateInstructions(LifecycleStopEvent, "SERVER_ONLY"));
    }

    private static InsnList generateClientInstructions(String event, LabelNode labelNode) {
        boolean inject = false;
        if (labelNode == null) {
            labelNode = new LabelNode();
            inject = true;
        }
        InsnList insnList = new InsnList();
        LabelNode isNotRemote = new LabelNode();
        insnList.add(new JumpInsnNode(IFEQ, isNotRemote));
        insnList.add(generateInstructions(event, "CLIENT_ONLY"));
        insnList.add(new JumpInsnNode(GOTO, labelNode));
        insnList.add(isNotRemote);
        insnList.add(generateInstructions(event, "SINGLE_PLAYER"));
        if (inject) {
            insnList.add(labelNode);
        }
        return insnList;
    }

    private static InsnList generateInstructions(String event, String type) {
        InsnList insnList = new InsnList();
        insnList.add(new FieldInsnNode(GETSTATIC, event, type, "L" + event + ";"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, event, "callEvent", "()V", false));
        return insnList;
    }
}
