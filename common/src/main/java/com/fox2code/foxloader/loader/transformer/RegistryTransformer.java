package com.fox2code.foxloader.loader.transformer;

import com.fox2code.foxloader.registry.GameRegistry;
import org.objectweb.asm.tree.*;

public class RegistryTransformer implements PreClassTransformer {
    private static final int MAXIMUM_BLOCK_ID = GameRegistry.MAXIMUM_BLOCK_ID;
    private static final int MAXIMUM_ITEM_ID = GameRegistry.MAXIMUM_ITEM_ID;
    private static final String BLOCK_CLS = "net.minecraft.src.game.block.Block";
    private static final String BLOCK = "net/minecraft/src/game/block/Block";
    private static final String ITEM = "net/minecraft/src/game/item/Item";

    @Override
    public void transform(ClassNode classNode, String className) {
        if (BLOCK_CLS.equals(className)) {
            patchBlock(classNode);
        }
    }

    private void patchBlock(ClassNode classNode) {
        FieldNode blockCap = TransformerUtils.getField(classNode, "blockcap");
        MethodNode clInit = TransformerUtils.getMethod(classNode, "<clinit>");
        int value = ((Number) blockCap.value).intValue();
        for (AbstractInsnNode insnNode : clInit.instructions) {
            if (insnNode.getOpcode() == SIPUSH) {
                IntInsnNode intInsnNode = (IntInsnNode) insnNode;
                int opcode;
                if (intInsnNode.operand == value && ((opcode =
                        intInsnNode.getNext().getOpcode()) == ANEWARRAY || opcode == NEWARRAY)) {
                    intInsnNode.operand = MAXIMUM_BLOCK_ID;
                }
            }
        }
    }
}
