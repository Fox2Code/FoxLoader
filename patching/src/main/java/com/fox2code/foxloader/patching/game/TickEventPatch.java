package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

final class TickEventPatch extends GamePatch {
    private static final String World = "net/minecraft/common/world/World";
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String MinecraftServer = "net/minecraft/server/MinecraftServer";
    private static final String GlobalTickEvent = "com/fox2code/foxloader/event/GlobalTickEvent";
    private static final String WorldTickEvent = "com/fox2code/foxloader/event/world/WorldTickEvent";
    private static final String ModLoader$Internal = "com/fox2code/foxloader/loader/ModLoader$Internal";

    TickEventPatch() {
        super(new String[]{World, Minecraft, MinecraftServer});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        boolean client;
        if (World.equals(classNode.name)) {
            classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_FINAL,
                    "tickEvent", "L" + WorldTickEvent + ";", null, null));
            for (MethodNode methodNode : classNode.methods) {
                if (!methodNode.name.equals("<init>")) continue;
                AbstractInsnNode abstractInsnNode = methodNode.instructions.getFirst();
                if (abstractInsnNode.getOpcode() == -1) {
                    abstractInsnNode = TransformerUtils.nextCodeInsn(abstractInsnNode);
                }
                if (abstractInsnNode.getOpcode() != ALOAD) continue;
                abstractInsnNode = TransformerUtils.nextCodeInsn(abstractInsnNode);
                if (abstractInsnNode.getOpcode() != INVOKESPECIAL) continue;
                abstractInsnNode = methodNode.instructions.getLast();
                if (abstractInsnNode.getOpcode() == -1) {
                    abstractInsnNode = TransformerUtils.previousCodeInsn(abstractInsnNode);
                }
                if (abstractInsnNode.getOpcode() != RETURN) throw new RuntimeException("WHAT?");
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(ALOAD, 0));
                insnList.add(new TypeInsnNode(NEW, WorldTickEvent));
                insnList.add(new InsnNode(DUP));
                insnList.add(new VarInsnNode(ALOAD, 0));
                insnList.add(new MethodInsnNode(INVOKESPECIAL, WorldTickEvent, "<init>", "(L" + World + ";)V"));
                insnList.add(new FieldInsnNode(PUTFIELD, World, "tickEvent", "L" + WorldTickEvent + ";"));
                methodNode.instructions.insertBefore(abstractInsnNode, insnList);
            }
            MethodNode tick = TransformerUtils.getMethod(classNode, "tick");
            AbstractInsnNode firstInsn = tick.instructions.getFirst();
            if (firstInsn.getOpcode() == -1) firstInsn = TransformerUtils.nextCodeInsn(firstInsn);
            InsnList insnList = new InsnList();
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new FieldInsnNode(GETFIELD, World, "tickEvent", "L" + WorldTickEvent + ";"));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, WorldTickEvent, "callEvent", "()V"));
            tick.instructions.insertBefore(firstInsn, insnList);
        } else if ((client = Minecraft.equals(classNode.name)) ||
                MinecraftServer.equals(classNode.name)) {
            MethodNode tick = TransformerUtils.getMethod(classNode, client ? "runTick" : "doTick");
            AbstractInsnNode firstInsn = tick.instructions.getFirst();
            if (firstInsn.getOpcode() == -1) firstInsn = TransformerUtils.nextCodeInsn(firstInsn);
            InsnList tickEventCode = new InsnList();
            tickEventCode.add(new FieldInsnNode(GETSTATIC, GlobalTickEvent, "INSTANCE", "L" + GlobalTickEvent + ";"));
            tickEventCode.add(new MethodInsnNode(INVOKEVIRTUAL, GlobalTickEvent, "callEvent", "()V"));
            tick.instructions.insertBefore(firstInsn, tickEventCode);
            MethodNode init = TransformerUtils.getMethod(classNode, client ? "startGame" : "startServer");
            AbstractInsnNode lastInsn = init.instructions.getLast();
            if (lastInsn.getOpcode() == -1) lastInsn = TransformerUtils.previousCodeInsn(lastInsn);
            if (lastInsn.getOpcode() == IRETURN) {
                lastInsn = TransformerUtils.previousCodeInsn(lastInsn);
                lastInsn = TransformerUtils.previousCodeInsn(lastInsn);
                lastInsn = TransformerUtils.previousCodeInsn(lastInsn);
                lastInsn = TransformerUtils.previousCodeInsn(lastInsn);
                if (lastInsn.getOpcode() != ALOAD) {
                    throw new RuntimeException("WHAT?");
                }
            } else if (lastInsn.getOpcode() != RETURN) {
                throw new RuntimeException("WHAT?");
            }
            init.instructions.insertBefore(lastInsn, new MethodInsnNode(
                    INVOKESTATIC, ModLoader$Internal, "postInitializeMods", "()V", false));
        }
        return classNode;
    }
}
