package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.util.Objects;

// TODO: Patch doExplosionKnockback
final class ExplosionPatch extends GamePatch {
    private static final String Blocks = "net/minecraft/common/block/Blocks";
    private static final String BlockFire = "net/minecraft/common/block/BlockFire";
    private static final String Explosion = "net/minecraft/common/world/Explosion";
    private static final String DirectedExplosion = "net/minecraft/common/world/DirectedExplosion";
    private static final String World = "net/minecraft/common/world/World";
    private static final String ChunkPosition = "net/minecraft/common/world/chunk/ChunkPosition";
    private static final String BlockChange = "com/fox2code/foxloader/event/world/WorldMultiBlockChange$BlockChange";
    private static final String InternalExplosionHooks = "com/fox2code/foxloader/internal/InternalExplosionHooks";

    ExplosionPatch() {
        super(new String[]{Explosion, DirectedExplosion});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (classNode.name.equals(Explosion)) {
            classNode = remapExplosion(classNode);
            TransformerUtils.makeFieldPublic(classNode, "worldObj");
            TransformerUtils.makeFieldPublic(classNode, "ExplosionRNG");
            classNode.fields.add(new FieldNode(ACC_PUBLIC, "silent", "Z", null, null));
            patchDoExplosionA(classNode);
            patchDoExplosionB(classNode);
        } else if (classNode.name.equals(DirectedExplosion)) {
            classNode = remapExplosion(classNode);
            // patchDoExplosionA(classNode);
        }
        return classNode;
    }

    private static ClassNode remapExplosion(ClassNode classNode) {
        ClassNode newClassNode = new ClassNode();
        classNode.accept(new ClassRemapper(newClassNode, new Remapper() {
            @Override
            public String map(String internalName) {
                return ChunkPosition.equals(internalName) ? BlockChange : internalName;
            }
        }));
        return newClassNode;
    }

    private static void patchDoExplosionA(ClassNode classNode) {
        // Patch doExplosionA
        MethodNode doExplosionA = TransformerUtils.findMethod(classNode, "createExplosionFire");
        if (doExplosionA == null) {
            doExplosionA = TransformerUtils.getMethod(classNode, "doExplosionA");
        }
        patchExplosionMethod(doExplosionA, true);
    }

    private static void patchDoExplosionB(ClassNode classNode) {
        // Patch doExplosionB
        MethodNode doExplosionB = TransformerUtils.getMethod(classNode, "doExplosionB");
        AbstractInsnNode firstCode = TransformerUtils.nextCodeInsn(doExplosionB.instructions.getFirst());
        LabelNode skipSound = null;
        for (AbstractInsnNode abstractInsnNode : doExplosionB.instructions) {
            if (abstractInsnNode.getOpcode() == INVOKEVIRTUAL &&
                    "playSoundEffect".equals(((MethodInsnNode) abstractInsnNode).name)) {
                skipSound = TransformerUtils.getLabelNodeAfter(doExplosionB.instructions, abstractInsnNode);
                break;
            }
        }
        Objects.requireNonNull(skipSound, "skipSound");
        InsnList earlyHook = new InsnList();
        earlyHook.add(new VarInsnNode(ALOAD, 0));
        earlyHook.add(new VarInsnNode(ALOAD, 0));
        earlyHook.add(new FieldInsnNode(GETFIELD, Explosion, "destroyedBlockPositions", "L" + Set + ";"));
        earlyHook.add(new MethodInsnNode(INVOKESTATIC, InternalExplosionHooks,
                "onSendExplosionB", "(L" + Explosion + ";L" + Collection + ";)Z"));
        LabelNode notCancelled = new LabelNode();
        earlyHook.add(new JumpInsnNode(IFEQ, notCancelled));
        earlyHook.add(new InsnNode(RETURN));
        earlyHook.add(notCancelled);
        earlyHook.add(new VarInsnNode(ALOAD, 0));
        earlyHook.add(new FieldInsnNode(GETFIELD, Explosion, "silent", "Z"));
        earlyHook.add(new JumpInsnNode(IFNE, skipSound));
        doExplosionB.instructions.insertBefore(firstCode, earlyHook);
        patchExplosionMethod(doExplosionB, false);
    }

    private static void patchExplosionMethod(MethodNode explosionMethod, boolean isExplosionA) {
        int blockChangeVar = -1;
        boolean checkCst = false;
        MethodInsnNode setBlockWithNotify = null;
        for (AbstractInsnNode abstractInsnNode : explosionMethod.instructions) {
            if (abstractInsnNode.getOpcode() == CHECKCAST) {
                checkCst = BlockChange.equals(((TypeInsnNode) abstractInsnNode).desc);
            } else if (abstractInsnNode.getOpcode() == ASTORE && checkCst) {
                blockChangeVar = ((VarInsnNode)abstractInsnNode).var;
                checkCst = false;
            } else if (abstractInsnNode.getOpcode() == INVOKEVIRTUAL && blockChangeVar != -1 &&
                    ((MethodInsnNode) abstractInsnNode).name.equals("setBlockWithNotify")) {
                setBlockWithNotify = (MethodInsnNode) abstractInsnNode;
                break;
            }
        }
        Objects.requireNonNull(setBlockWithNotify, "setBlockWithNotify");
        if (isExplosionA) {
            AbstractInsnNode startA = TransformerUtils.previousNonCodeInsn(setBlockWithNotify);
            AbstractInsnNode endA = TransformerUtils.nextNonCodeInsn(setBlockWithNotify);
            TransformerUtils.removeInstructionsInRange(explosionMethod.instructions, startA, endA);
            InsnList fireBlockMod = new InsnList();
            fireBlockMod.add(new VarInsnNode(ALOAD, blockChangeVar));
            fireBlockMod.add(new FieldInsnNode(GETSTATIC, Blocks, "FIRE", "L" + BlockFire + ";"));
            fireBlockMod.add(new FieldInsnNode(GETFIELD, BlockFire, "blockID", "I"));
            fireBlockMod.add(new FieldInsnNode(PUTFIELD, BlockChange, "newId", "I"));
            explosionMethod.instructions.insert(startA, fireBlockMod);
        } else {
            MethodInsnNode setBlockAndMetadataWithNotify = new MethodInsnNode(
                    INVOKEVIRTUAL, World, "setBlockAndMetadataWithNotify", "(IIIII)Z");
            explosionMethod.instructions.insert(setBlockWithNotify, setBlockAndMetadataWithNotify);
            explosionMethod.instructions.remove(setBlockWithNotify);
            AbstractInsnNode previous = setBlockAndMetadataWithNotify.getPrevious();
            if (previous.getOpcode() == GETFIELD) {
                explosionMethod.instructions.remove(previous.getPrevious());
            }
            explosionMethod.instructions.remove(previous);
            InsnList getIdAndMeta = new InsnList();
            getIdAndMeta.add(new VarInsnNode(ALOAD, blockChangeVar));
            getIdAndMeta.add(new FieldInsnNode(GETFIELD, BlockChange, "newId", "I"));
            getIdAndMeta.add(new VarInsnNode(ALOAD, blockChangeVar));
            getIdAndMeta.add(new FieldInsnNode(GETFIELD, BlockChange, "newMeta", "I"));
            explosionMethod.instructions.insertBefore(setBlockAndMetadataWithNotify, getIdAndMeta);
        }
    }
}
