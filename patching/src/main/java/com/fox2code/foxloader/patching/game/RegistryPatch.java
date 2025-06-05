/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.PatchConstants;
import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Objects;

final class RegistryPatch extends GamePatch {
    private static final int ORIGINAL_BLOCK_LIMIT = PatchConstants.ORIGINAL_BLOCK_LIMIT;
    private static final int MODIFIED_BLOCK_LIMIT = PatchConstants.MODIFIED_BLOCK_LIMIT;
    private static final int INITIAL_BLOCK_ID = PatchConstants.INITIAL_BLOCK_ID;
    private static final int BLOCK_ID_DIFF = PatchConstants.BLOCK_ID_DIFF;
    private static final Integer ORIGINAL_BLOCK_LIMIT_OBJ = ORIGINAL_BLOCK_LIMIT;
    private static final Integer MODIFIED_BLOCK_LIMIT_OBJ = MODIFIED_BLOCK_LIMIT;
    private static final String ItemStack = "net/minecraft/common/item/ItemStack";
    private static final String Item = "net/minecraft/common/item/Item";
    private static final String Items = "net/minecraft/common/item/Items";
    private static final String ItemBlock = "net/minecraft/common/item/block/ItemBlock";
    private static final String ItemBlockBasalt = "net/minecraft/common/item/block/ItemBlockBasalt";
    private static final String ItemBlockBrimstone = "net/minecraft/common/item/block/ItemBlockBrimstone";
    private static final String ItemBlockCobblestone = "net/minecraft/common/item/block/ItemBlockCobblestone";
    private static final String ItemBlockColored = "net/minecraft/common/item/block/ItemBlockColored";
    private static final String ItemBlockCrate = "net/minecraft/common/item/block/ItemBlockCrate";
    private static final String ItemBlockCrateColored = "net/minecraft/common/item/block/ItemBlockCrateColored";
    private static final String ItemBlockDrawer = "net/minecraft/common/item/block/ItemBlockDrawer";
    private static final String ItemBlockFence = "net/minecraft/common/item/block/ItemBlockFence";
    private static final String ItemBlockFlower = "net/minecraft/common/item/block/ItemBlockFlower";
    private static final String ItemBlockHoneycomb = "net/minecraft/common/item/block/ItemBlockHoneycomb";
    private static final String ItemBlockLadder = "net/minecraft/common/item/block/ItemBlockLadder";
    private static final String ItemBlockLayer = "net/minecraft/common/item/block/ItemBlockLayer";
    private static final String ItemBlockLeaves = "net/minecraft/common/item/block/ItemBlockLeaves";
    private static final String ItemBlockLeavesNether = "net/minecraft/common/item/block/ItemBlockLeavesNether";
    private static final String ItemBlockLog = "net/minecraft/common/item/block/ItemBlockLog";
    private static final String ItemBlockPiston = "net/minecraft/common/item/block/ItemBlockPiston";
    private static final String ItemBlockPlanks = "net/minecraft/common/item/block/ItemBlockPlanks";
    private static final String ItemBlockRedCoral = "net/minecraft/common/item/block/ItemBlockRedCoral";
    private static final String ItemBlockSlab = "net/minecraft/common/item/block/ItemBlockSlab";
    private static final String ItemRecord = "net/minecraft/common/item/children/ItemRecord";
    private static final String Block = "net/minecraft/common/block/Block";
    private static final String Blocks = "net/minecraft/common/block/Blocks";
    private static final String BlockBasalt = "net/minecraft/common/block/children/BlockBasalt";
    private static final String BlockBrimstone = "net/minecraft/common/block/children/BlockBrimstone";
    private static final String BlockCobblestone = "net/minecraft/common/block/children/BlockCobblestone";
    private static final String BlockColored = "net/minecraft/common/block/children/BlockColored";
    private static final String BlockCrate = "net/minecraft/common/block/children/BlockCrate";
    private static final String BlockCrateColored = "net/minecraft/common/block/children/BlockCrateColored";
    private static final String BlockDrawer = "net/minecraft/common/block/children/BlockDrawer";
    private static final String BlockFence = "net/minecraft/common/block/children/BlockFence";
    private static final String BlockFlower = "net/minecraft/common/block/children/BlockFlower";
    private static final String BlockHoneycomb = "net/minecraft/common/block/children/BlockHoneycomb";
    private static final String BlockLadder = "net/minecraft/common/block/children/BlockLadder";
    private static final String BlockLayer = "net/minecraft/common/block/children/BlockLayer";
    private static final String BlockLeavesBase = "net/minecraft/common/block/children/BlockLeavesBase";
    private static final String BlockLeavesBaseOpaque = "net/minecraft/common/block/children/BlockLeavesBaseOpaque";
    private static final String BlockLog = "net/minecraft/common/block/children/BlockLog";
    private static final String BlockPistonBase = "net/minecraft/common/block/children/BlockPistonBase";
    private static final String BlockPlanks = "net/minecraft/common/block/children/BlockPlanks";
    private static final String BlockRedCoral = "net/minecraft/common/block/children/BlockRedCoral";
    private static final String BlockSlab = "net/minecraft/common/block/children/BlockSlab";
    private static final String ChunkBlockMap = "net/minecraft/common/world/chunk/ChunkBlockMap";
    private static final String Material = "net/minecraft/common/block/data/Material";
    private static final String Packet = "net/minecraft/common/networking/Packet";
    private static final String RenderItem = "net/minecraft/client/renderer/entity/RenderItem";
    private static final String GuiConnecting = "net/minecraft/client/gui/GuiConnecting";
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String GameRegistry = "com/fox2code/foxloader/registry/GameRegistry";
    private static final String GameRegistry$Internal = "com/fox2code/foxloader/registry/GameRegistry$Internal";
    private static final String ModContainer = "com/fox2code/foxloader/loader/ModContainer";
    private static final String CreativeTab = "com/fox2code/foxloader/client/CreativeTab";

    RegistryPatch() {
        super(ALL_CLASSES);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        boolean skipGeneric = false;
        String genericMeta = null;
        switch (classNode.name) {
            case ChunkBlockMap: {
                patchChunkBlockMap(classNode);
                break;
            }
            case GuiConnecting: {
                patchGuiConnecting(classNode);
                break;
            }
            case Minecraft: {
                patchMinecraft(classNode);
                break;
            }
            case ItemStack: {
                patchItemStackNet(classNode);
                return classNode; // <- Skip Insn patching on ItemStack
            }
            case Items: {
                skipGeneric = true;
                break;
            }
            case Item: {
                patchItem(classNode);
                skipGeneric = true;
                break;
            }
            case ItemBlock: {
                patchItemBlock(classNode);
                skipGeneric = true;
                break;
            }
            case ItemBlockSlab: {
                patchItemBlockSlab(classNode);
                skipGeneric = true;
                break;
            }
            case Blocks: {
                patchBlocks(classNode);
                skipGeneric = true;
                break;
            }
            case Block: {
                patchBlock(classNode);
                skipGeneric = true;
                break;
            }
            case BlockBasalt: {
                genericMeta = ItemBlockBasalt;
                break;
            }
            case BlockBrimstone: {
                genericMeta = ItemBlockBrimstone;
                break;
            }
            case BlockCobblestone: {
                genericMeta = ItemBlockCobblestone;
                break;
            }
            case BlockColored: {
                genericMeta = ItemBlockColored;
                break;
            }
            case BlockCrate: {
                genericMeta = ItemBlockCrate;
                break;
            }
            case BlockCrateColored: {
                genericMeta = ItemBlockCrateColored;
                break;
            }
            case BlockDrawer: {
                genericMeta = ItemBlockDrawer;
                break;
            }
            case BlockFence: {
                genericMeta = ItemBlockFence;
                break;
            }
            case BlockFlower: {
                genericMeta = ItemBlockFlower;
                break;
            }
            case BlockHoneycomb: {
                genericMeta = ItemBlockHoneycomb;
                break;
            }
            case BlockLadder: {
                genericMeta = ItemBlockLadder;
                break;
            }
            case BlockLayer: {
                genericMeta = ItemBlockLayer;
                break;
            }
            case BlockLeavesBase: {
                genericMeta = ItemBlockLeaves;
                break;
            }
            case BlockLeavesBaseOpaque: {
                genericMeta = ItemBlockLeavesNether;
                break;
            }
            case BlockLog: {
                genericMeta = ItemBlockLog;
                break;
            }
            case BlockPistonBase: {
                genericMeta = ItemBlockPiston;
                break;
            }
            case BlockPlanks: {
                genericMeta = ItemBlockPlanks;
                break;
            }
            case BlockRedCoral: {
                genericMeta = ItemBlockRedCoral;
                break;
            }
            case BlockSlab: {
                patchBlockSlab(classNode);
                break;
            }
            case Packet: {
                patchPacket(classNode);
                skipGeneric = true;
                break;
            }
            case RenderItem: {
                patchRenderItem(classNode);
                break;
            }
        }
        if ((classNode.access & (ACC_ENUM | ACC_INTERFACE)) != 0 ||
                "java/lang/Object".equals(classNode.superName)) {
            skipGeneric = true;
        }
        if ((!skipGeneric) && ((classNode.name.startsWith("net/minecraft/common/item/children/Item") &&
                classNode.name.indexOf('/', "net/minecraft/common/item/children/Item".length()) == -1) ||
                classNode.name.startsWith("net/minecraft/common/item/block/ItemBlock"))) {
            patchItemGeneric(classNode);
        }
        if ((!skipGeneric) && classNode.name.startsWith("net/minecraft/common/block/children/Block") &&
                classNode.name.indexOf('/', "net/minecraft/common/block/children/Block".length()) == -1) {
            patchBlockGeneric(classNode, genericMeta);
        }
        boolean remote = classNode.name.startsWith("net/minecraft/common/networking/");
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
                if (abstractInsnNode == null) continue; // <- Manage invalid bytecode
                int opcode = abstractInsnNode.getOpcode();
                if (opcode != GETFIELD && opcode != PUTFIELD) continue;
                FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                if (fieldInsnNode.owner.equals(ItemStack) && fieldInsnNode.name.equals("itemID")) {
                    if (opcode == GETFIELD) {
                        methodNode.instructions.insert(fieldInsnNode, new MethodInsnNode(
                                INVOKEVIRTUAL, ItemStack, remote ? "getRemoteItemID" : "getItemID", "()I"));
                        methodNode.instructions.remove(fieldInsnNode);
                    } else {
                        methodNode.instructions.insert(fieldInsnNode, new MethodInsnNode(
                                INVOKEVIRTUAL, ItemStack, remote ? "setRemoteItemID" : "setItemID", "(I)V"));
                        methodNode.instructions.remove(fieldInsnNode);
                    }
                }
            }
        }
        return classNode;
    }

    // Helpers
    private static void injectGetRegisterFLTab(ClassNode classNode, String creativeTab) {
        MethodNode getRegisterFLTab = new MethodNode(ASM_API,
                ACC_PUBLIC, "getRegisterFLTab", "()L" + CreativeTab + ";", null, null);
        getRegisterFLTab.instructions.add(new FieldInsnNode(
                GETSTATIC, CreativeTab, creativeTab, "L" + CreativeTab + ";"));
        getRegisterFLTab.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getRegisterFLTab);
    }

    // Data loss prevention
    private static void patchChunkBlockMap(ClassNode classNode) {
        classNode.fields.clear();
        classNode.methods.clear();
        MethodNode methodNode = new MethodNode(ASM_API, ACC_PUBLIC | ACC_STATIC,
                "cleanupInvalidBlocks", "([S)V", null, null);
        LabelNode startCode = new LabelNode();
        methodNode.instructions.add(startCode);
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        LabelNode ifNonNull = new LabelNode();
        methodNode.instructions.add(new JumpInsnNode(IFNONNULL, ifNonNull));
        methodNode.instructions.add(new InsnNode(RETURN));
        methodNode.instructions.add(ifNonNull);
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        methodNode.instructions.add(new InsnNode(ARRAYLENGTH));
        methodNode.instructions.add(new VarInsnNode(ISTORE, 1));
        LabelNode lenStart = new LabelNode();
        methodNode.instructions.add(lenStart);
        methodNode.instructions.add(new InsnNode(ICONST_0));
        methodNode.instructions.add(new VarInsnNode(ISTORE, 2));
        LabelNode iStart = new LabelNode();
        methodNode.instructions.add(iStart);
        LabelNode startLoop = new LabelNode();
        methodNode.instructions.add(startLoop);
        methodNode.instructions.add(new VarInsnNode(ILOAD, 2));
        methodNode.instructions.add(new VarInsnNode(ILOAD, 1));
        LabelNode endLoop = new LabelNode();
        methodNode.instructions.add(new JumpInsnNode(IF_ICMPGE, endLoop));
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        methodNode.instructions.add(new VarInsnNode(ILOAD, 2));
        methodNode.instructions.add(new InsnNode(SALOAD));
        methodNode.instructions.add(TransformerUtils.getNumberInsn(65535));
        methodNode.instructions.add(new InsnNode(IAND));
        methodNode.instructions.add(new VarInsnNode(ISTORE, 3));
        LabelNode blockStart = new LabelNode();
        methodNode.instructions.add(blockStart);
        methodNode.instructions.add(new VarInsnNode(ILOAD, 3));
        methodNode.instructions.add(TransformerUtils.getNumberInsn(PatchConstants.MODIFIED_BLOCK_LIMIT));
        LabelNode blockIdValid = new LabelNode();
        methodNode.instructions.add(new JumpInsnNode(IF_ICMPLT, blockIdValid));
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        methodNode.instructions.add(new VarInsnNode(ILOAD, 2));
        methodNode.instructions.add(new InsnNode(ICONST_0));
        methodNode.instructions.add(new InsnNode(SASTORE));
        methodNode.instructions.add(blockIdValid);
        methodNode.instructions.add(new IincInsnNode(2, 1));
        methodNode.instructions.add(new JumpInsnNode(GOTO, startLoop));
        methodNode.instructions.add(endLoop);
        methodNode.instructions.add(new InsnNode(RETURN));
        LabelNode endCode = new LabelNode();
        methodNode.instructions.add(endCode);
        methodNode.localVariables.add(new LocalVariableNode("block", "I", null, blockStart, blockIdValid, 3));
        methodNode.localVariables.add(new LocalVariableNode("i", "I", null, iStart, endLoop, 2));
        methodNode.localVariables.add(new LocalVariableNode("bmap", "[S", null, startCode, endCode, 0));
        methodNode.localVariables.add(new LocalVariableNode("len", "I", null, lenStart, endCode, 1));
        methodNode.maxStack = 3;
        methodNode.maxLocals = 4;
        classNode.methods.add(methodNode);
    }

    // Network registry
    private static void patchGuiConnecting(ClassNode classNode) {
        MethodNode lambda$spawnNewServerThread$0 = TransformerUtils.getMethod(classNode, "lambda$spawnNewServerThread$0");
        InsnList insnList = new InsnList();
        insnList.add(new InsnNode(ICONST_0));
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry$Internal, "resetMappings", "(Z)V", false));
        TransformerUtils.insertToEndOfCode(lambda$spawnNewServerThread$0, insnList);
    }

    private static void patchMinecraft(ClassNode classNode) {
        MethodNode startWorld = TransformerUtils.getMethod(classNode, "startWorld");
        InsnList insnList = new InsnList();
        insnList.add(new InsnNode(ICONST_1));
        insnList.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry$Internal, "resetMappings", "(Z)V", false));
        TransformerUtils.insertToEndOfCode(startWorld, insnList);
    }

    private static void patchItemStackNet(ClassNode classNode) {
        classNode.fields.add(new FieldNode(ACC_PRIVATE, "netItemID", "I", null, null));
        TransformerUtils.getField(classNode, "itemID").access = ACC_PRIVATE;
        for (MethodNode methodNode : classNode.methods) {
            if (!"<init>".equals(methodNode.name)) continue;
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == PUTFIELD) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                    if ("itemID".equals(fieldInsnNode.name)) {
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(ALOAD, 0));
                        insnList.add(TransformerUtils.getNumberInsn(-1));
                        insnList.add(new FieldInsnNode(PUTFIELD, ItemStack, "netItemID", "I"));
                        methodNode.instructions.insert(fieldInsnNode, insnList);
                        break;
                    }
                }
            }
        }
        for (String methodPatch : new String[]{"readFromNBT", "writeToNBT", "stackHash"}) {
            MethodNode methodNode = TransformerUtils.getMethod(classNode, methodPatch);
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
                int opcode = abstractInsnNode.getOpcode();
                if (opcode != GETFIELD && opcode != PUTFIELD) continue;
                FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                if (fieldInsnNode.owner.equals(ItemStack) && fieldInsnNode.name.equals("itemID")) {
                    if (opcode == GETFIELD) {
                        methodNode.instructions.insert(fieldInsnNode, new MethodInsnNode(
                                INVOKEVIRTUAL, ItemStack, "getRemoteItemID", "()I"));
                        methodNode.instructions.remove(fieldInsnNode);
                    } else {
                        methodNode.instructions.insert(fieldInsnNode, new MethodInsnNode(
                                INVOKEVIRTUAL, ItemStack, "setRemoteItemID", "(I)V"));
                        methodNode.instructions.remove(fieldInsnNode);
                    }
                }
            }
        }
        for (String methodPatch : new String[]{"isStackEqual", "isItemStackEqual", "isItemEqual"}) {
            MethodNode methodNode = TransformerUtils.getMethod(classNode, methodPatch);
            boolean hasField = false;
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (hasField) {
                    if (abstractInsnNode.getOpcode() == IF_ICMPNE) {
                        JumpInsnNode jumpInsnNode = (JumpInsnNode) abstractInsnNode;
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(ALOAD, 0));
                        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                                ItemStack, "getRemoteItemID", "()I"));
                        insnList.add(new VarInsnNode(ALOAD, 1));
                        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                                ItemStack, "getRemoteItemID", "()I"));
                        insnList.add(new JumpInsnNode(IF_ICMPNE, jumpInsnNode.label));
                        methodNode.instructions.insert(jumpInsnNode, insnList);
                        break;
                    }
                } else {
                    if (abstractInsnNode.getOpcode() == GETFIELD) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                        if (fieldInsnNode.owner.equals(ItemStack) && fieldInsnNode.name.equals("itemID")) {
                            hasField = true;
                        }
                    }
                }
            }
        }
        MethodNode getItemID = new MethodNode(ACC_PUBLIC, "getItemID", "()I", null, null);
        classNode.methods.add(getItemID);
        getItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        getItemID.instructions.add(new FieldInsnNode(GETFIELD, ItemStack, "itemID", "I"));
        getItemID.instructions.add(new InsnNode(IRETURN));
        MethodNode getRemoteItemID = new MethodNode(ACC_PUBLIC, "getRemoteItemID", "()I", null, null);
        classNode.methods.add(getRemoteItemID);
        // Use netItemID field if available
        getRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        getRemoteItemID.instructions.add(new FieldInsnNode(GETFIELD, ItemStack, "netItemID", "I"));
        LabelNode useLocalNetItemID = new LabelNode();
        getRemoteItemID.instructions.add(TransformerUtils.getNumberInsn(-1));
        getRemoteItemID.instructions.add(new JumpInsnNode(IF_ICMPEQ, useLocalNetItemID));
        getRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        getRemoteItemID.instructions.add(new FieldInsnNode(GETFIELD, ItemStack, "netItemID", "I"));
        getRemoteItemID.instructions.add(new InsnNode(IRETURN));
        getRemoteItemID.instructions.add(useLocalNetItemID);
        // Check if ID is in FoxLoader registry range.
        getRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        getRemoteItemID.instructions.add(new FieldInsnNode(GETFIELD, ItemStack, "itemID", "I"));
        getRemoteItemID.instructions.add(TransformerUtils.getNumberInsn(PatchConstants.MAXIMUM_ITEM_ID));
        LabelNode idIsInRange = new LabelNode();
        getRemoteItemID.instructions.add(new JumpInsnNode(IF_ICMPLE, idIsInRange));
        getRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        getRemoteItemID.instructions.add(new FieldInsnNode(GETFIELD, ItemStack, "itemID", "I"));
        getRemoteItemID.instructions.add(new InsnNode(IRETURN));
        getRemoteItemID.instructions.add(idIsInRange);
        // Use mapping out to get remote ID
        getRemoteItemID.instructions.add(new FieldInsnNode(GETSTATIC, GameRegistry, "itemIdMappingOut", "[S"));
        getRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        getRemoteItemID.instructions.add(new FieldInsnNode(GETFIELD, ItemStack, "itemID", "I"));
        getRemoteItemID.instructions.add(new InsnNode(SALOAD));
        getRemoteItemID.instructions.add(new InsnNode(IRETURN));
        MethodNode setItemID = new MethodNode(ACC_PUBLIC, "setItemID", "(I)V", null, null);
        classNode.methods.add(setItemID);
        setItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        setItemID.instructions.add(new VarInsnNode(ILOAD, 1));
        setItemID.instructions.add(new FieldInsnNode(PUTFIELD, ItemStack, "itemID", "I"));
        setItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        setItemID.instructions.add(TransformerUtils.getNumberInsn(-1));
        setItemID.instructions.add(new FieldInsnNode(PUTFIELD, ItemStack, "netItemID", "I"));
        setItemID.instructions.add(new InsnNode(RETURN));
        TransformerUtils.setThisParameterName(classNode, setItemID);
        TransformerUtils.setParameterName(setItemID, 1, "itemID");
        MethodNode setRemoteItemID = new MethodNode(ACC_PUBLIC, "setRemoteItemID", "(I)V", null, null);
        classNode.methods.add(setRemoteItemID);
        // Check if ID is in FoxLoader registry range.
        setRemoteItemID.instructions.add(new VarInsnNode(ILOAD, 1));
        setRemoteItemID.instructions.add(TransformerUtils.getNumberInsn(PatchConstants.MAXIMUM_ITEM_ID));
        LabelNode idIsInRange2 = new LabelNode();
        setRemoteItemID.instructions.add(new JumpInsnNode(IF_ICMPLE, idIsInRange2));
        setRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        setRemoteItemID.instructions.add(new VarInsnNode(ILOAD, 1));
        setRemoteItemID.instructions.add(new FieldInsnNode(PUTFIELD, ItemStack, "itemID", "I"));
        setRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        setRemoteItemID.instructions.add(TransformerUtils.getNumberInsn(-1));
        setRemoteItemID.instructions.add(new FieldInsnNode(PUTFIELD, ItemStack, "netItemID", "I"));
        setRemoteItemID.instructions.add(new InsnNode(RETURN));
        setRemoteItemID.instructions.add(idIsInRange2);
        // Set remote item ID
        setRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        setRemoteItemID.instructions.add(new FieldInsnNode(GETSTATIC, GameRegistry, "itemIdMappingIn", "[S"));
        setRemoteItemID.instructions.add(new VarInsnNode(ILOAD, 1));
        setRemoteItemID.instructions.add(new InsnNode(SALOAD));
        setRemoteItemID.instructions.add(new FieldInsnNode(PUTFIELD, ItemStack, "itemID", "I"));
        setRemoteItemID.instructions.add(new VarInsnNode(ALOAD, 0));
        setRemoteItemID.instructions.add(new VarInsnNode(ILOAD, 1));
        setRemoteItemID.instructions.add(new FieldInsnNode(PUTFIELD, ItemStack, "netItemID", "I"));
        setRemoteItemID.instructions.add(new InsnNode(RETURN));
        TransformerUtils.setThisParameterName(classNode, setRemoteItemID);
        TransformerUtils.setParameterName(setRemoteItemID, 1, "netItemID");
    }

    private static void patchPacket(ClassNode classNode) {
        MethodNode readItemStack = TransformerUtils.getMethod(classNode, "readItemStack");
        boolean step1 = false;
        boolean step2 = false;
        boolean success = false;
        for (AbstractInsnNode abstractInsnNode : readItemStack.instructions.toArray()) {
            if (abstractInsnNode.getOpcode() == ILOAD) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                if (varInsnNode.var == 2) {
                    if (step1 && !step2) {
                        step2 = true;
                        readItemStack.instructions.insert(varInsnNode, TransformerUtils.getNumberInsn(0));
                        readItemStack.instructions.remove(varInsnNode);
                    } else step1 = true;
                }
            } else if (step2 && abstractInsnNode.getOpcode() == ALOAD) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                if (varInsnNode.var == 1 && !success) {
                    AbstractInsnNode previous = varInsnNode.getPrevious();
                    success = true;
                    InsnList insnList = new InsnList();
                    insnList.add(new VarInsnNode(ALOAD, 1));
                    insnList.add(new VarInsnNode(ILOAD, 2));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                            ItemStack, "setRemoteItemID", "(I)V"));
                    readItemStack.instructions.insertBefore(previous, insnList);
                }
            }
        }
    }

    // Render patching
    private static void patchRenderItem(ClassNode classNode) {
        MethodNode renderItemEntity = TransformerUtils.getMethod(classNode, "renderItemEntity");
        AbstractInsnNode isItemRecord = null;
        VarInsnNode iStore = null;
        JumpInsnNode jump = null;
        for (AbstractInsnNode abstractInsnNode : renderItemEntity.instructions) {
            if (abstractInsnNode.getOpcode() == INSTANCEOF &&
                    ItemRecord.equals(((TypeInsnNode) abstractInsnNode).desc)) {
                isItemRecord = abstractInsnNode;
            } else if (abstractInsnNode.getOpcode() == ISTORE && isItemRecord != null && iStore == null) {
                iStore = (VarInsnNode) abstractInsnNode;
            } else if (abstractInsnNode.getOpcode() == IF_ICMPLE && iStore != null) {
                jump = (JumpInsnNode) abstractInsnNode;
            }
        }
        Objects.requireNonNull(isItemRecord, "isItemRecord");
        Objects.requireNonNull(iStore, "iStore");
        Objects.requireNonNull(jump, "jump");
        AbstractInsnNode prevBlockCheck = TransformerUtils.previousNonCodeInsn(isItemRecord);
        TransformerUtils.removeInstructionsInRange(renderItemEntity.instructions, prevBlockCheck, iStore);
        InsnList newBlockCheck = new InsnList();
        newBlockCheck.add(new VarInsnNode(ILOAD, 7));
        newBlockCheck.add(new MethodInsnNode(INVOKESTATIC, GameRegistry, "isItemBlock", "(I)Z"));
        renderItemEntity.instructions.insertBefore(iStore, newBlockCheck);
        AbstractInsnNode prevFixID = TransformerUtils.previousNonCodeInsn(jump);
        AbstractInsnNode nextFixID = jump.label;
        TransformerUtils.removeInstructionsInRange(renderItemEntity.instructions, prevFixID, nextFixID);
        InsnList toBlockId = new InsnList();
        toBlockId.add(new VarInsnNode(ILOAD, 7));
        toBlockId.add(new MethodInsnNode(INVOKESTATIC, GameRegistry, "convertItemIdToBlockId", "(I)I"));
        toBlockId.add(new VarInsnNode(ISTORE, 7));
        renderItemEntity.instructions.insert(prevFixID, toBlockId);
    }

    // Block/Item patching
    private static void patchItem(ClassNode classNode) {
        TransformerUtils.getMethod(classNode, "setMaxDamage").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "setWearable").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "addDescription").access = ACC_PUBLIC;
        MethodNode init = TransformerUtils.getMethod(classNode, "<init>");
        MethodNode flInit = TransformerUtils.copyMethodNode(init);
        init.access |= ACC_DEPRECATED;
        flInit.access = ACC_PUBLIC;
        flInit.desc = "(Ljava/lang/String;)V";
        InsnList checkVanillaConstructorItem = new InsnList();
        checkVanillaConstructorItem.add(new VarInsnNode(ALOAD, 0));
        checkVanillaConstructorItem.add(new VarInsnNode(ILOAD, 1));
        checkVanillaConstructorItem.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry$Internal, "checkVanillaConstructorItem", "(L" + Item + ";I)V", false));
        TransformerUtils.insertAfterConstructor(init, checkVanillaConstructorItem);
        for (LocalVariableNode localVariableNode : flInit.localVariables) {
            if (localVariableNode.index == 1) {
                localVariableNode.index = 2;
                flInit.localVariables.add(new LocalVariableNode("name", "Ljava/lang/String;",
                        null, localVariableNode.start, localVariableNode.end, 1));
                break;
            }
        }
        for (AbstractInsnNode abstractInsnNode : flInit.instructions.toArray()) {
            if (abstractInsnNode.getType() == AbstractInsnNode.VAR_INSN) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                if (varInsnNode.var == 1) {
                    varInsnNode.var = 2;
                }
            } else if (abstractInsnNode.getOpcode() == IADD ||
                    abstractInsnNode.getOpcode() == SIPUSH) {
                flInit.instructions.remove(abstractInsnNode);
            }
        }
        AbstractInsnNode abstractInsnNode = flInit.instructions.getFirst();
        while (!(abstractInsnNode.getOpcode() == PUTFIELD &&
                ((FieldInsnNode) abstractInsnNode).name.equals("burnType"))) {
            abstractInsnNode = TransformerUtils.nextCodeInsn(abstractInsnNode);
        }
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ALOAD, 1));
        insnList.add(new MethodInsnNode(INVOKESTATIC, GameRegistry$Internal,
                "generateItemId", "(L" + Item + ";Ljava/lang/String;)I"));
        insnList.add(new VarInsnNode(ISTORE, 2));
        flInit.instructions.insert(abstractInsnNode, insnList);
        TransformerUtils.addMethodBefore(classNode, "<init>", flInit);
        MethodNode getStringItemIDByName = TransformerUtils.getMethod(classNode, "getStringItemIDByName");
        int index = getStringItemIDByName.maxLocals;
        for (LocalVariableNode localVariableNode : getStringItemIDByName.localVariables) {
            if (localVariableNode.index == 0) {
                getStringItemIDByName.localVariables.add(new LocalVariableNode("regItemId",
                        "Ljava/lang/String;", null, localVariableNode.start, localVariableNode.end, index));
                break;
            }
        }
        getStringItemIDByName.maxLocals++;
        InsnList getStringItemIDByNamePrelude = new InsnList();
        getStringItemIDByNamePrelude.add(new VarInsnNode(ALOAD, 0));
        getStringItemIDByNamePrelude.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry$Internal, "strRegIdToItemStrId", "(Ljava/lang/String;)Ljava/lang/String;", false));
        getStringItemIDByNamePrelude.add(new VarInsnNode(ASTORE, 0));
        getStringItemIDByNamePrelude.add(new VarInsnNode(ALOAD, 0));
        LabelNode noMatch = new LabelNode();
        getStringItemIDByNamePrelude.add(new JumpInsnNode(IFNULL, noMatch));
        getStringItemIDByNamePrelude.add(new VarInsnNode(ALOAD, 0));
        getStringItemIDByNamePrelude.add(new InsnNode(ARETURN));
        getStringItemIDByNamePrelude.add(noMatch);
        TransformerUtils.insertToBeginningOfCode(getStringItemIDByName, getStringItemIDByNamePrelude);
        // getRegisteringMod
        MethodNode getRegisteringMod = new MethodNode(ASM_API,
                ACC_PUBLIC | ACC_FINAL, "getRegisteringMod", "()L" + ModContainer + ";", null, null);
        InsnList getRegisteringModInsns = getRegisteringMod.instructions;
        getRegisteringModInsns.add(new VarInsnNode(ALOAD, 0));
        getRegisteringModInsns.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry, "getRegisteringMod", "(L" + Item + ";)L" + ModContainer + ";"));
        getRegisteringModInsns.add(new InsnNode(ARETURN));
        classNode.methods.add(getRegisteringMod);
        // getRegisterFLTab
        injectGetRegisterFLTab(classNode, "MISCELLANEOUS");
    }

    private static void patchItemBlock(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("<init>")) {
                methodNode.access |= ACC_DEPRECATED;
            }
        }
        MethodNode flInit = new MethodNode(ASM_API, ACC_PUBLIC, "<init>", "(L" + Block + ";)V", null, null);
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        flInit.access = ACC_PUBLIC;
        flInit.localVariables.add(new LocalVariableNode("block", "L" + Block + ";", null, start, end, 1));
        flInit.instructions.add(start);
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, Block, "getItemID", "()I", false));
        flInit.instructions.add(TransformerUtils.getNumberInsn(256));
        flInit.instructions.add(new InsnNode(ISUB));
        flInit.instructions.add(new MethodInsnNode(INVOKESPECIAL, Item, "<init>", "(I)V", false));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new FieldInsnNode(GETFIELD, Block, "blockID", "I"));
        flInit.instructions.add(new FieldInsnNode(PUTFIELD, ItemBlock, "blockID", "I"));
        flInit.instructions.add(new InsnNode(RETURN));
        flInit.instructions.add(end);
        TransformerUtils.addMethodBefore(classNode, "<init>", flInit);
        // getRegisterFLTab
        MethodNode getRegisterFLTab = new MethodNode(ASM_API,
                ACC_PUBLIC, "getRegisterFLTab", "()L" + CreativeTab + ";", null, null);
        getRegisterFLTab.instructions.add(new FieldInsnNode(
                GETSTATIC, Blocks, "BLOCKS_LIST", "[L" + Block + ";"));
        getRegisterFLTab.instructions.add(new VarInsnNode(ALOAD, 0));
        getRegisterFLTab.instructions.add(new FieldInsnNode(
                GETFIELD, ItemBlock, "blockID", "I"));
        getRegisterFLTab.instructions.add(new InsnNode(AALOAD));
        getRegisterFLTab.instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL, Block, "getRegisterFLTab", "()L" + CreativeTab + ";", false));
        getRegisterFLTab.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getRegisterFLTab);
    }

    private static void patchBlock(ClassNode classNode) {
        TransformerUtils.getField(classNode, "blockMaterial").access = ACC_PUBLIC;
        TransformerUtils.getField(classNode, "blockName").access = ACC_PROTECTED;
        TransformerUtils.getMethod(classNode, "getBlockName").access |= ACC_FINAL;
        TransformerUtils.getMethod(classNode, "setBlockName").access |= ACC_FINAL;
        TransformerUtils.getMethod(classNode, "setLightOpacity").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "setResistance").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "setIsIndestructible").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "setBlockUnbreakable").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "setHardness").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "disableStats").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "setWearable").access = ACC_PUBLIC;
        TransformerUtils.getMethod(classNode, "addDescription").access = ACC_PUBLIC;
        MethodNode init = TransformerUtils.getMethod(classNode, "<init>");
        MethodNode flInit = TransformerUtils.copyMethodNode(init);
        init.access |= ACC_DEPRECATED;
        flInit.access = ACC_PUBLIC;
        flInit.desc = "(Ljava/lang/String;L" + Material + ";)V";
        InsnList checkVanillaConstructorBlock = new InsnList();
        checkVanillaConstructorBlock.add(new VarInsnNode(ALOAD, 0));
        checkVanillaConstructorBlock.add(new VarInsnNode(ILOAD, 1));
        checkVanillaConstructorBlock.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry$Internal, "checkVanillaConstructorBlock", "(L" + Block + ";I)V", false));
        TransformerUtils.insertAfterConstructor(init, checkVanillaConstructorBlock);
        for (LocalVariableNode localVariableNode : flInit.localVariables) {
            if (localVariableNode.index == 1) {
                localVariableNode.index = 3;
                flInit.localVariables.add(new LocalVariableNode("name", "Ljava/lang/String;",
                        null, localVariableNode.start, localVariableNode.end, 1));
                break;
            }
        }
        for (AbstractInsnNode abstractInsnNode : flInit.instructions.toArray()) {
            if (abstractInsnNode.getType() == AbstractInsnNode.VAR_INSN) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                if (varInsnNode.var == 1) {
                    varInsnNode.var = 3;
                }
            }
        }
        AbstractInsnNode lastFieldSet = flInit.instructions.getFirst();
        while (!(lastFieldSet.getOpcode() == PUTFIELD &&
                ((FieldInsnNode) lastFieldSet).name.equals("slipperiness"))) {
            lastFieldSet = TransformerUtils.nextCodeInsn(lastFieldSet);
        }
        InsnList initPrefix = new InsnList();
        initPrefix.add(new VarInsnNode(ALOAD, 0));
        initPrefix.add(new VarInsnNode(ALOAD, 1));
        initPrefix.add(new MethodInsnNode(INVOKESTATIC, GameRegistry$Internal,
                "generateBlockId", "(L" + Block + ";Ljava/lang/String;)I"));
        initPrefix.add(new VarInsnNode(ISTORE, 3));
        flInit.instructions.insert(lastFieldSet, initPrefix);
        TransformerUtils.addMethodBefore(classNode, "<init>", flInit);
        MethodNode getItemID = TransformerUtils.getMethod(classNode, "getItemID");
        LabelNode getItemIDGoto = null;
        for (AbstractInsnNode abstractInsnNode : getItemID.instructions) {
            if (abstractInsnNode.getOpcode() == GOTO) {
                getItemIDGoto = ((JumpInsnNode) abstractInsnNode).label;
                break;
            }
        }
        InsnList blockIdPrefix = new InsnList();
        blockIdPrefix.add(new VarInsnNode(ALOAD, 0));
        blockIdPrefix.add(new FieldInsnNode(GETFIELD, Block, "blockID", "I"));
        blockIdPrefix.add(TransformerUtils.getNumberInsn(INITIAL_BLOCK_ID));
        LabelNode jmp = new LabelNode();
        blockIdPrefix.add(new JumpInsnNode(IF_ICMPLT, jmp));
        blockIdPrefix.add(new VarInsnNode(ALOAD, 0));
        blockIdPrefix.add(new FieldInsnNode(GETFIELD, Block, "blockID", "I"));
        blockIdPrefix.add(TransformerUtils.getNumberInsn(BLOCK_ID_DIFF));
        blockIdPrefix.add(new InsnNode(IADD));
        if (getItemIDGoto != null) {
            blockIdPrefix.add(new JumpInsnNode(GOTO, getItemIDGoto));
        } else {
            blockIdPrefix.add(new InsnNode(IRETURN));
        }
        blockIdPrefix.add(jmp);
        TransformerUtils.insertToBeginningOfCode(getItemID, blockIdPrefix);
        // getItemID.access |= ACC_FINAL; // ReIndev need to override BLock.getItemID()
        MethodNode initializeItemBlock = new MethodNode(ASM_API, ACC_PROTECTED,
                "initializeItemBlock", "()L" + ItemBlock + ";", null, null);
        initializeItemBlock.instructions.add(new TypeInsnNode(NEW, ItemBlock));
        initializeItemBlock.instructions.add(new InsnNode(DUP));
        initializeItemBlock.instructions.add(new VarInsnNode(ALOAD, 0));
        initializeItemBlock.instructions.add(new MethodInsnNode(
                INVOKESPECIAL, ItemBlock, "<init>", "(L" + Block + ";)V"));
        initializeItemBlock.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(initializeItemBlock);
        MethodNode internalInitializeItemBlock = new MethodNode(ASM_API, ACC_STATIC | ACC_PUBLIC,
                "internalInitializeItemBlock", "(L" + Block + ";)Z", null, null);
        LabelNode internalInitializeItemBlockStart = new LabelNode();
        LabelNode internalInitializeItemBlockEnd = new LabelNode();
        internalInitializeItemBlock.localVariables.add(new LocalVariableNode("block", "L" + Block + ";",
                null, internalInitializeItemBlockStart, internalInitializeItemBlockEnd, 0));
        internalInitializeItemBlock.localVariables.add(new LocalVariableNode("itemID", "I",
                null, internalInitializeItemBlockStart, internalInitializeItemBlockEnd, 1));
        internalInitializeItemBlock.instructions.add(internalInitializeItemBlockStart);
        internalInitializeItemBlock.instructions.add(new VarInsnNode(ALOAD, 0));
        internalInitializeItemBlock.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                Block, "getItemID", "()I"));
        internalInitializeItemBlock.instructions.add(new VarInsnNode(ISTORE, 1));
        internalInitializeItemBlock.instructions.add(new VarInsnNode(ALOAD, 0));
        internalInitializeItemBlock.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                Block, "initializeItemBlock", "()L" + ItemBlock + ";"));
        internalInitializeItemBlock.instructions.add(new FieldInsnNode(GETFIELD, ItemBlock, "itemID", "I"));
        internalInitializeItemBlock.instructions.add(new VarInsnNode(ILOAD, 1));
        LabelNode everythingIsFine = new LabelNode();
        internalInitializeItemBlock.instructions.add(new JumpInsnNode(IF_ICMPEQ, everythingIsFine));
        internalInitializeItemBlock.instructions.add(new InsnNode(ICONST_0));
        internalInitializeItemBlock.instructions.add(new InsnNode(IRETURN));
        internalInitializeItemBlock.instructions.add(everythingIsFine);
        internalInitializeItemBlock.instructions.add(new InsnNode(ICONST_1));
        internalInitializeItemBlock.instructions.add(new InsnNode(IRETURN));
        internalInitializeItemBlock.instructions.add(internalInitializeItemBlockEnd);
        classNode.methods.add(internalInitializeItemBlock);
        for (boolean next : new boolean[]{false, true}) {
            MethodNode nextPreviousRegisteredBlock = new MethodNode(ASM_API, ACC_PUBLIC | ACC_FINAL,
                    next ? "nextRegisteredBlock" : "previousRegisteredBlock", "()L" + Block + ";", null, null);
            InsnList insnList = nextPreviousRegisteredBlock.instructions;
            insnList.add(new FieldInsnNode(GETSTATIC, Blocks, "BLOCKS_LIST", "[L" + Block + ";"));
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new FieldInsnNode(GETFIELD, Block, "blockId", "I"));
            insnList.add(TransformerUtils.getNumberInsn(1));
            insnList.add(new InsnNode(next ? IADD : ISUB));
            insnList.add(new InsnNode(AALOAD));
            insnList.add(new InsnNode(ARETURN));
            classNode.methods.add(nextPreviousRegisteredBlock);
        }
        MethodNode getStringItemIDByName = TransformerUtils.getMethod(classNode, "getBlockByName");
        int index = getStringItemIDByName.maxLocals;
        for (LocalVariableNode localVariableNode : getStringItemIDByName.localVariables) {
            if (localVariableNode.index == 0) {
                getStringItemIDByName.localVariables.add(new LocalVariableNode("regBlockId",
                        "Ljava/lang/String;", null, localVariableNode.start, localVariableNode.end, index));
                break;
            }
        }
        getStringItemIDByName.maxLocals++;
        InsnList getStringItemIDByNamePrelude = new InsnList();
        getStringItemIDByNamePrelude.add(new VarInsnNode(ALOAD, 0));
        getStringItemIDByNamePrelude.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry$Internal, "strRegIdToBlockStrId", "(Ljava/lang/String;)Ljava/lang/String;", false));
        getStringItemIDByNamePrelude.add(new VarInsnNode(ASTORE, 0));
        getStringItemIDByNamePrelude.add(new VarInsnNode(ALOAD, 0));
        LabelNode noMatch = new LabelNode();
        getStringItemIDByNamePrelude.add(new JumpInsnNode(IFNULL, noMatch));
        getStringItemIDByNamePrelude.add(new VarInsnNode(ALOAD, 0));
        getStringItemIDByNamePrelude.add(new InsnNode(ARETURN));
        getStringItemIDByNamePrelude.add(noMatch);
        TransformerUtils.insertToBeginningOfCode(getStringItemIDByName, getStringItemIDByNamePrelude);
        // getRegisteringMod
        MethodNode getRegisteringMod = new MethodNode(ASM_API,
                ACC_PUBLIC | ACC_FINAL, "getRegisteringMod", "()L" + ModContainer + ";", null, null);
        InsnList getRegisteringModInsns = getRegisteringMod.instructions;
        getRegisteringModInsns.add(new VarInsnNode(ALOAD, 0));
        getRegisteringModInsns.add(new MethodInsnNode(INVOKESTATIC,
                GameRegistry, "getRegisteringMod", "(L" + Block + ";)L" + ModContainer + ";"));
        getRegisteringModInsns.add(new InsnNode(ARETURN));
        classNode.methods.add(getRegisteringMod);
        // getRegisterFLTab
        injectGetRegisterFLTab(classNode, "MISCELLANEOUS");
    }

    private static void patchBlocks(ClassNode classNode) {
        for (FieldNode fieldNode : classNode.fields) {
            if (ORIGINAL_BLOCK_LIMIT_OBJ.equals(fieldNode.value)) {
                fieldNode.value = MODIFIED_BLOCK_LIMIT_OBJ;
            }
        }
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode.getOpcode() == SIPUSH) {
                    IntInsnNode intInsnNode = (IntInsnNode) abstractInsnNode;
                    if (intInsnNode.operand == ORIGINAL_BLOCK_LIMIT) {
                        intInsnNode.operand = MODIFIED_BLOCK_LIMIT;
                    }
                } else if (abstractInsnNode.getOpcode() == INVOKESPECIAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.owner.equals(Block)) break;
                }
            }
        }
    }

    // Slab handling
    private static void patchItemBlockSlab(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("<init>")) {
                methodNode.access |= ACC_DEPRECATED;
            }
        }
        MethodNode flInit = new MethodNode(ASM_API, ACC_PUBLIC, "<init>", "(L" + BlockSlab + ";)V", null, null);
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        flInit.localVariables.add(new LocalVariableNode("block", "L" + Block + ";", null, start, end, 1));
        flInit.instructions.add(start);
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new MethodInsnNode(INVOKESPECIAL,
                ItemBlock, "<init>", "(L" + Block + ";)V", false));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new FieldInsnNode(GETFIELD,
                BlockSlab, "isDoubleSlab", "Z"));
        flInit.instructions.add(new FieldInsnNode(PUTFIELD,
                ItemBlockSlab, "isFullBlock", "Z"));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new FieldInsnNode(GETFIELD,
                ItemBlockSlab, "isFullBlock", "Z"));
        LabelNode isNotFull = new LabelNode();
        LabelNode endIsNotFull = new LabelNode();
        flInit.instructions.add(new JumpInsnNode(IFEQ, isNotFull));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                BlockSlab, "previousRegisteredBlock", "()L" + Block + ";", false));
        flInit.instructions.add(new TypeInsnNode(CHECKCAST, BlockSlab));
        flInit.instructions.add(new FieldInsnNode(PUTFIELD,
                ItemBlockSlab, "half", "L" + BlockSlab + ";"));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new FieldInsnNode(PUTFIELD,
                ItemBlockSlab, "full", "L" + BlockSlab + ";"));
        flInit.instructions.add(new JumpInsnNode(GOTO, endIsNotFull));
        flInit.instructions.add(isNotFull);
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new FieldInsnNode(PUTFIELD,
                ItemBlockSlab, "half", "L" + BlockSlab + ";"));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(new VarInsnNode(ALOAD, 1));
        flInit.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                BlockSlab, "nextRegisteredBlock", "()L" + Block + ";", false));
        flInit.instructions.add(new TypeInsnNode(CHECKCAST, BlockSlab));
        flInit.instructions.add(new FieldInsnNode(PUTFIELD,
                ItemBlockSlab, "full", "L" + BlockSlab + ";"));
        flInit.instructions.add(endIsNotFull);
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(TransformerUtils.getNumberInsn(0));
        flInit.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                ItemBlockSlab, "setMaxDamage", "(I)L" + Item + ";", false));
        flInit.instructions.add(new InsnNode(POP));
        flInit.instructions.add(new VarInsnNode(ALOAD, 0));
        flInit.instructions.add(TransformerUtils.getBooleanInsn(true));
        flInit.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                ItemBlockSlab, "setHasSubtypes", "(Z)L" + Item + ";", false));
        flInit.instructions.add(new InsnNode(POP));
        flInit.instructions.add(new InsnNode(RETURN));
        flInit.instructions.add(end);
        TransformerUtils.addMethodBefore(classNode, "<init>", flInit);
    }

    private static void patchBlockSlab(ClassNode classNode) {
        TransformerUtils.getField(classNode, "isDoubleSlab").access = (ACC_FINAL | ACC_PUBLIC);
        MethodNode initializeItemBlock = new MethodNode(ASM_API, ACC_PROTECTED,
                "initializeItemBlock", "()L" + ItemBlock + ";", null, null);
        initializeItemBlock.instructions.add(new TypeInsnNode(NEW, ItemBlockSlab));
        initializeItemBlock.instructions.add(new InsnNode(DUP));
        initializeItemBlock.instructions.add(new VarInsnNode(ALOAD, 0));
        initializeItemBlock.instructions.add(new MethodInsnNode(
                INVOKESPECIAL, ItemBlockSlab, "<init>", "(L" + BlockSlab + ";)V"));
        initializeItemBlock.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(initializeItemBlock);
    }

    // Generic

    private static void patchItemGeneric(ClassNode classNode) {
        boolean isItemBlock = (!classNode.superName.equals(Item)) &&
                (classNode.name.contains("ItemBlock") || classNode.superName.equals(ItemBlock));
        ArrayList<MethodNode> initializers = new ArrayList<>(4);
        boolean gotOneValidConstructor = false;
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("<init>")) {
                if (!methodNode.desc.startsWith("(I")) {
                    throw new RuntimeException("ReIndev Quirk need manual intervention for " + classNode.name);
                }
                if ((methodNode.access & ACC_PUBLIC) != 0)
                    methodNode.access |= ACC_DEPRECATED;
                methodNode.access &= ~ACC_PROTECTED;
                boolean useBlockVarShift = false;
                boolean useIdVarShift = false;
                if (isItemBlock) {
                    if (methodNode.desc.startsWith("(II")) {
                        boolean hasBlockIdArg = false;
                        for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                            if (localVariableNode.index == 2) {
                                String lowercaseName = localVariableNode.name.toLowerCase(Locale.ROOT);
                                hasBlockIdArg = "blid".equals(lowercaseName) ||
                                        "block_id".equals(lowercaseName) ||
                                        "blockId".equals(lowercaseName) ||
                                        "blockid".equals(lowercaseName);
                                break;
                            }
                        }
                        if (hasBlockIdArg) {
                            String clashingDesc1 = methodNode.desc.replace("(II", "(I");
                            String clashingDesc2 = methodNode.desc.replace("(II", "(IL" + Block + ";");
                            if (TransformerUtils.findMethod(classNode, "<init>", clashingDesc1) != null ||
                                    TransformerUtils.findMethod(classNode, "<init>", clashingDesc2) != null) {
                                continue; // We will use the simpler constructor instead
                            }
                            useIdVarShift = true;
                        }
                    } else if (methodNode.desc.startsWith("(IL" + Block + ";")) {
                        useBlockVarShift = true;
                    } else {
                        String clashingDesc = methodNode.desc.replace("(I", "(IL" + Block + ";");
                        if (TransformerUtils.findMethod(classNode, "<init>", clashingDesc) != null) {
                            continue; // We will use the constructor providing a block instead
                        }
                    }
                }
                MethodNode flInit = TransformerUtils.copyMethodNode(methodNode);
                flInit.access = ACC_PUBLIC;
                initializers.add(flInit);
                if (useIdVarShift) {
                    flInit.desc = methodNode.desc.replace("(II", "(L" + Block + ";");
                } else if (useBlockVarShift) {
                    flInit.desc = methodNode.desc.replace("(IL" + Block + ";", "(L" + Block + ";");
                } else if (isItemBlock) {
                    flInit.desc = methodNode.desc.replace("(I", "(L" + Block + ";");
                } else {
                    flInit.desc = methodNode.desc.replace("(I", "(Ljava/lang/String;");
                }
                AbstractInsnNode constructor = TransformerUtils.nextCodeInsn(flInit.instructions.getFirst());
                while (constructor.getOpcode() != INVOKESPECIAL) {
                    constructor = TransformerUtils.nextCodeInsn(constructor);
                }
                MethodInsnNode methodInsnConstructor = (MethodInsnNode) constructor;
                boolean blockIdItemIdCall =
                        ItemBlock.equals(methodInsnConstructor.owner) &&
                        "(II)V".equals(methodInsnConstructor.desc);
                boolean useNormalParamInSpecial = false;
                if (blockIdItemIdCall) {
                    if (!(useBlockVarShift || useIdVarShift)) {
                        // We should never reach there
                        throw new RuntimeException("Block Id ItemCall, " +
                                "but no blockId param on " + classNode.name + "?");
                    }
                } else {
                    useNormalParamInSpecial = useBlockVarShift;
                }
                for (ListIterator<LocalVariableNode> iterator = flInit.localVariables.listIterator(); iterator.hasNext();) {
                    LocalVariableNode localVariableNode = iterator.next();
                    if (localVariableNode.index == 0) continue;
                    if (useBlockVarShift) {
                        if (localVariableNode.index == 1) {
                            iterator.remove();
                        } else {
                            localVariableNode.index--;
                        }
                    } else if (useIdVarShift) {
                        if (localVariableNode.index == 1) {
                            iterator.set(new LocalVariableNode("block", "L" + Block + ";", null,
                                    localVariableNode.start, localVariableNode.end, localVariableNode.index));
                        } else if (localVariableNode.index == 2) {
                            iterator.remove();
                        } else {
                            localVariableNode.index--;
                        }
                    } else {
                        if (localVariableNode.index == 1) {
                            iterator.set(isItemBlock ?
                                    new LocalVariableNode("block", "L" + Block + ";", null,
                                            localVariableNode.start, localVariableNode.end, localVariableNode.index) :
                                    new LocalVariableNode("id", "Ljava/lang/String;", null,
                                            localVariableNode.start, localVariableNode.end, localVariableNode.index));
                        }
                    }
                }
                boolean beforeConstructor = true;
                boolean specialHandling = useBlockVarShift || useIdVarShift;
                for (AbstractInsnNode abstractInsnNode : flInit.instructions.toArray()) {
                    if (abstractInsnNode.getType() == AbstractInsnNode.VAR_INSN) {
                        VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                        if (specialHandling && varInsnNode.var != 0) {
                            if (varInsnNode.var == 1) {
                                if (useNormalParamInSpecial && beforeConstructor) {
                                    // A bit unsafe...
                                    flInit.instructions.insert(varInsnNode, new VarInsnNode(ALOAD, 1));
                                    flInit.instructions.remove(varInsnNode);
                                } else {
                                    InsnList insnList = new InsnList();
                                    insnList.add(new VarInsnNode(ALOAD, 1));
                                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, Block, "getItemID", "()I", false));
                                    insnList.add(TransformerUtils.getNumberInsn(256));
                                    insnList.add(new InsnNode(ISUB));
                                    flInit.instructions.insert(varInsnNode, insnList);
                                    flInit.instructions.remove(varInsnNode);
                                }
                            } else if (useIdVarShift && varInsnNode.var == 2) {
                                InsnList insnList = new InsnList();
                                insnList.add(new VarInsnNode(ALOAD, 1));
                                insnList.add(new FieldInsnNode(GETFIELD, Block, "blockID", "I"));
                                flInit.instructions.insert(varInsnNode, insnList);
                                flInit.instructions.remove(varInsnNode);
                            } else {
                                varInsnNode.var--;
                            }
                        } else if (varInsnNode.var == 1) {
                            if (beforeConstructor) {
                                // A bit unsafe...
                                flInit.instructions.insert(varInsnNode, new VarInsnNode(ALOAD, 1));
                                flInit.instructions.remove(varInsnNode);
                            } else {
                                InsnList insnList = new InsnList();
                                insnList.add(new VarInsnNode(ALOAD, 0));
                                insnList.add(new FieldInsnNode(GETFIELD, classNode.name, "itemID", "I"));
                                insnList.add(TransformerUtils.getNumberInsn(256));
                                insnList.add(new InsnNode(ISUB));
                                flInit.instructions.insert(varInsnNode, insnList);
                                flInit.instructions.remove(varInsnNode);
                            }
                        }
                    } else if (abstractInsnNode.getOpcode() == INVOKESPECIAL && beforeConstructor) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                        if (methodInsnNode.owner.equals(classNode.name) ||
                                methodInsnNode.owner.equals(classNode.superName)) {
                            if (useNormalParamInSpecial || !specialHandling) {
                                methodInsnNode.desc = methodInsnNode.desc.replace(
                                        "(I", isItemBlock ? "(L" + Block + ";" : "(Ljava/lang/String;");
                            }
                            beforeConstructor = false;
                        }
                    }
                }
            }
        }
        ListIterator<MethodNode> initializerIterator =
                initializers.listIterator(initializers.size());
        while (initializerIterator.hasPrevious()) {
            TransformerUtils.addMethodBefore(classNode,
                    "<init>", initializerIterator.previous());
        }
    }

    private static void patchBlockGeneric(ClassNode classNode, String genericMeta) {
        ArrayList<MethodNode> initializers = new ArrayList<>(16);
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.name.equals("<init>")) continue;
            if ((methodNode.access & ACC_PUBLIC) != 0)
                methodNode.access |= ACC_DEPRECATED;
            methodNode.access &= ~ACC_PROTECTED;
            if (!methodNode.desc.startsWith("(I")) continue;
            MethodNode flInit = TransformerUtils.copyMethodNode(methodNode);
            initializers.add(flInit);
            flInit.access = ACC_PUBLIC;
            flInit.desc = methodNode.desc.replace("(I", "(Ljava/lang/String;");
            for (ListIterator<LocalVariableNode> iterator = flInit.localVariables.listIterator(); iterator.hasNext();) {
                LocalVariableNode localVariableNode = iterator.next();
                if (localVariableNode.index == 1) {
                    iterator.set(new LocalVariableNode("id", "Ljava/lang/String;", null,
                            localVariableNode.start, localVariableNode.end, localVariableNode.index));
                }
            }
            boolean beforeConstructor = true;
            for (AbstractInsnNode abstractInsnNode : flInit.instructions.toArray()) {
                if (abstractInsnNode.getType() == AbstractInsnNode.VAR_INSN) {
                    VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                    if (varInsnNode.var == 1) {
                        if (beforeConstructor) {
                            // A bit unsafe...
                            flInit.instructions.insert(varInsnNode, new VarInsnNode(ALOAD, 1));
                            flInit.instructions.remove(varInsnNode);
                        } else {
                            InsnList insnList = new InsnList();
                            insnList.add(new VarInsnNode(ALOAD, 0));
                            insnList.add(new FieldInsnNode(GETFIELD, classNode.name, "blockID", "I"));
                            flInit.instructions.insert(varInsnNode, insnList);
                            flInit.instructions.remove(varInsnNode);
                        }
                    }
                } else if (abstractInsnNode.getOpcode() == INVOKESPECIAL && beforeConstructor) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                    if (methodInsnNode.owner.equals(classNode.name) ||
                            methodInsnNode.owner.equals(classNode.superName)) {
                        methodInsnNode.desc = methodInsnNode.desc.replace("(I", "(Ljava/lang/String;");
                        beforeConstructor = false;
                    }
                }
            }
        }
        ListIterator<MethodNode> initializerIterator =
                initializers.listIterator(initializers.size());
        while (initializerIterator.hasPrevious()) {
            TransformerUtils.addMethodBefore(classNode,
                    "<init>", initializerIterator.previous());
        }
        if (genericMeta != null) {
            MethodNode initializeItemBlock = new MethodNode(ASM_API, ACC_PROTECTED,
                    "initializeItemBlock", "()L" + ItemBlock + ";", null, null);
            initializeItemBlock.instructions.add(new TypeInsnNode(NEW, genericMeta));
            initializeItemBlock.instructions.add(new InsnNode(DUP));
            initializeItemBlock.instructions.add(new VarInsnNode(ALOAD, 0));
            initializeItemBlock.instructions.add(new MethodInsnNode(
                    INVOKESPECIAL, genericMeta, "<init>", "(L" + Block + ";)V"));
            initializeItemBlock.instructions.add(new InsnNode(ARETURN));
            classNode.methods.add(initializeItemBlock);
        }
    }
}
