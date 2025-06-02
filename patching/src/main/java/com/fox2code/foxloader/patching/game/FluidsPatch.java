package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

import java.util.Objects;

final class FluidsPatch extends GamePatch {
    private static final String World = "net/minecraft/common/world/World";
    private static final String BlockFluid = "net/minecraft/common/block/children/BlockFluid";
    private static final String InternalFluidsHooks = "com/fox2code/foxloader/internal/InternalFluidsHooks";

    FluidsPatch() {
        super(BlockFluid);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (BlockFluid.equals(classNode.name)) {
            patchBlockFluid(classNode);
        }
        return classNode;
    }

    private static void patchBlockFluid(ClassNode classNode) {
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "flowIntoBlock");
        JumpInsnNode jumpInstance = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == IFEQ) {
                jumpInstance = (JumpInsnNode) abstractInsnNode;
                break;
            }
        }
        Objects.requireNonNull(jumpInstance);
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 1));
        insnList.add(new VarInsnNode(ILOAD, 2));
        insnList.add(new VarInsnNode(ILOAD, 3));
        insnList.add(new VarInsnNode(ILOAD, 4));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ILOAD, 5));
        insnList.add(new MethodInsnNode(INVOKESTATIC, InternalFluidsHooks,
                "onLiquidFlowIntoBlock", "(L" + World + ";IIIL" + BlockFluid + ";I)Z", false));
        insnList.add(new JumpInsnNode(IFNE, jumpInstance.label));
        methodNode.instructions.insert(jumpInstance, insnList);
    }
}
