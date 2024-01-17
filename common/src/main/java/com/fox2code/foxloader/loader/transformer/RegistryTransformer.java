package com.fox2code.foxloader.loader.transformer;

import com.fox2code.foxloader.registry.GameRegistry;
import org.objectweb.asm.tree.*;

public class RegistryTransformer implements PreClassTransformer {
    private static final int MAXIMUM_BLOCK_ID = GameRegistry.MAXIMUM_BLOCK_ID;
    private static final int MAXIMUM_ITEM_ID = GameRegistry.MAXIMUM_ITEM_ID;
    private static final String BLOCK_CLS = "net.minecraft.src.game.block.Block";
    private static final String ITEM_MAP_CLS = "net.minecraft.src.game.item.ItemMap";
    private static final String GAME_REGISTRY = "com/fox2code/foxloader/registry/GameRegistry";
    private static final String BLOCK = "net/minecraft/src/game/block/Block";
    private static final String ITEM = "net/minecraft/src/game/item/Item";
    private static final String BITSET = "java/util/BitSet";

    private int oldBlockcap = -1;

    @Override
    public void transform(ClassNode classNode, String className) {
        if (BLOCK_CLS.equals(className)) {
            patchBlock(classNode);
        } else if (ITEM_MAP_CLS.equals(className)) {
            patchItemMap(classNode);
        }
    }

    private void patchBlock(ClassNode classNode) {
        FieldNode blockCap = TransformerUtils.getField(classNode, "blockcap");
        MethodNode clInit = TransformerUtils.getMethod(classNode, "<clinit>");
        int oldBlockcap = this.oldBlockcap =
                ((Number) blockCap.value).intValue();
        blockCap.value = MAXIMUM_BLOCK_ID;
        for (AbstractInsnNode insnNode : clInit.instructions) {
            if (insnNode.getOpcode() == SIPUSH) {
                IntInsnNode intInsnNode = (IntInsnNode) insnNode;
                int opcode;
                AbstractInsnNode next;
                if (intInsnNode.operand == oldBlockcap && ((opcode =
                        (next = intInsnNode.getNext()).getOpcode()) == ANEWARRAY || opcode == NEWARRAY ||
                        (opcode == INVOKESPECIAL && ((MethodInsnNode) next).owner.equals(BITSET)))) {
                    intInsnNode.operand = MAXIMUM_BLOCK_ID;
                }
            }
        }
    }

    private void patchItemMap(ClassNode classNode) {
        int oldBlockcap = this.oldBlockcap;
        if (oldBlockcap == -1) {
            throw new IllegalStateException("ItemMap loaded before blocks?");
        }

        for (MethodNode methodNode : classNode.methods) {
            InsnList insnList = methodNode.instructions;
            for (AbstractInsnNode insnNode : insnList) {
                if ((insnNode.getOpcode() == NEWARRAY && ((IntInsnNode) insnNode).operand == T_INT) ||
                        (insnNode.getOpcode() == INVOKESPECIAL && ((MethodInsnNode) insnNode).owner.equals(BITSET))) {
                    AbstractInsnNode previous = insnNode.getPrevious();
                    if (previous instanceof IntInsnNode &&
                            ((IntInsnNode) previous).operand == oldBlockcap) {
                        insnList.insert(insnNode,
                                new MethodInsnNode(INVOKESTATIC, GAME_REGISTRY,
                                        "getTemporaryBlockIntArray", "()[I"));
                        insnList.remove(previous);
                        insnList.remove(insnNode);
                    }
                }
            }
        }
    }
}
