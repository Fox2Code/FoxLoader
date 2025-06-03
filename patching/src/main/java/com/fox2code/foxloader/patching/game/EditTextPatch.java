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

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Objects;

final class EditTextPatch extends GamePatch {
    private static final String GuiEditSign = "net/minecraft/client/gui/GuiEditSign";
    private static final String GuiEditCuneiformBlock = "net/minecraft/client/gui/GuiEditCuneiformBlock";
    private static final String NetServerHandler = "net/minecraft/server/networking/NetServerHandler";
    private static final String Packet130UpdateSign = "net/minecraft/common/networking/Packet130UpdateSign";
    private static final String Packet133UpdateCuneiformBlock = "net/minecraft/common/networking/Packet133UpdateCuneiformBlock";
    private static final String TileEntitySign = "net/minecraft/common/block/tileentity/TileEntitySign";
    private static final String TileEntityCuneiformBlock = "net/minecraft/common/block/tileentity/TileEntityCuneiformBlock";
    private static final String InternalEditTextHooks = "com/fox2code/foxloader/internal/InternalEditTextHooks";

    EditTextPatch() {
        super(new String[]{GuiEditSign, GuiEditCuneiformBlock, NetServerHandler});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case GuiEditSign: {
                patchGuiEditSign(classNode);
                break;
            }
            case GuiEditCuneiformBlock: {
                patchGuiEditCuneiformBlock(classNode);
                break;
            }
            case NetServerHandler: {
                patchNetServerHandler(classNode);
                break;
            }
        }
        return classNode;
    }

    public static void patchGuiEditSign(ClassNode classNode) {
        // Add Field
        FieldNode previousSignText = new FieldNode(ACC_PRIVATE | ACC_FINAL,
                "previousSignText", "[Ljava/lang/String;", null, null);
        FieldNode signTileEntity =
                TransformerUtils.getFieldDesc(classNode, "L" + TileEntitySign + ";");
        TransformerUtils.addFieldAfter(classNode, signTileEntity.name, previousSignText);
        // Util updateLastUnprocessedText InsnList
        InsnList updateLastUnprocessedText = new InsnList();
        updateLastUnprocessedText.add(new VarInsnNode(ALOAD, 0));
        updateLastUnprocessedText.add(new FieldInsnNode(GETFIELD, classNode.name,
                signTileEntity.name, signTileEntity.desc));
        updateLastUnprocessedText.add(new FieldInsnNode(GETFIELD, TileEntitySign,
                "signText", "[Ljava/lang/String;"));
        updateLastUnprocessedText.add(TransformerUtils.getNumberInsn(0));
        updateLastUnprocessedText.add(new VarInsnNode(ALOAD, 0));
        updateLastUnprocessedText.add(new FieldInsnNode(GETFIELD, classNode.name,
                previousSignText.name, previousSignText.desc));
        updateLastUnprocessedText.add(TransformerUtils.getNumberInsn(0));
        updateLastUnprocessedText.add(TransformerUtils.getNumberInsn(4));
        updateLastUnprocessedText.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System",
                "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false));
        // Init the previousSignText field
        MethodNode init = TransformerUtils.getMethod(classNode, "<init>");
        InsnList initInsnList = new InsnList();
        initInsnList.add(new VarInsnNode(ALOAD, 0));
        initInsnList.add(TransformerUtils.getNumberInsn(4));
        initInsnList.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        initInsnList.add(new FieldInsnNode(PUTFIELD, classNode.name,
                previousSignText.name, previousSignText.desc));
        initInsnList.add(TransformerUtils.copyInsnList(updateLastUnprocessedText));
        TransformerUtils.insertToEndOfCode(init, initInsnList);
        TransformerUtils.makeGetterForFields(classNode, "previousSignText");
        // Call the event when onGuiClosed is called
        MethodNode onGuiClosed = TransformerUtils.getMethod(classNode, "onGuiClosed");
        AbstractInsnNode enableRepeatEventsCall = null;
        for (AbstractInsnNode abstractInsnNode : onGuiClosed.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.INVOKESTATIC &&
                    ((MethodInsnNode) abstractInsnNode).name.equals("enableRepeatEvents")) {
                enableRepeatEventsCall = abstractInsnNode;
                break;
            }
        }
        InsnList handleHead = new InsnList();
        handleHead.add(new VarInsnNode(ALOAD, 0));
        handleHead.add(new VarInsnNode(ALOAD, 0));
        handleHead.add(new FieldInsnNode(GETFIELD, classNode.name,
                signTileEntity.name, signTileEntity.desc));
        handleHead.add(new MethodInsnNode(INVOKESTATIC, InternalEditTextHooks, "handleLocalSignEdit",
                "(L" + classNode.name + ";L" + TileEntitySign + ";)V"));
        if (enableRepeatEventsCall == null) {
            TransformerUtils.insertToBeginningOfCode(onGuiClosed, handleHead);
        } else {
            onGuiClosed.instructions.insert(enableRepeatEventsCall, handleHead);
        }
    }

    public static void patchGuiEditCuneiformBlock(ClassNode classNode) {
        // Add getText/setText methods
        TransformerUtils.makeGetterForFields(classNode, "text");
        MethodNode setText = new MethodNode(ACC_PUBLIC, "setText", "(Ljava/lang/String;)V", null, null);
        setText.instructions.add(new VarInsnNode(ALOAD, 0));
        setText.instructions.add(new VarInsnNode(ALOAD, 1));
        setText.instructions.add(new FieldInsnNode(PUTFIELD, GuiEditCuneiformBlock, "text", "Ljava/lang/String;"));
        setText.instructions.add(new InsnNode(RETURN));
        classNode.methods.add(setText);
        // Handle calls
        FieldNode cuneiformTileEntity =
                TransformerUtils.getFieldDesc(classNode, "L" + TileEntityCuneiformBlock + ";");
        MethodNode sendTextToServer = TransformerUtils.getMethod(classNode, "sendTextToServer");
        AbstractInsnNode attemptToGenerateDlaTextCall = null;
        for (AbstractInsnNode abstractInsnNode : sendTextToServer.instructions) {
            final int opcode = abstractInsnNode.getOpcode();
            if ((opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) &&
                    ((MethodInsnNode) abstractInsnNode).name.equals("attemptToGenerateDlaText")) {
                attemptToGenerateDlaTextCall = abstractInsnNode;
                break;
            }
        }
        InsnList handleHead = new InsnList();
        handleHead.add(new VarInsnNode(ALOAD, 0));
        handleHead.add(new VarInsnNode(ALOAD, 0));
        handleHead.add(new FieldInsnNode(GETFIELD,
                GuiEditCuneiformBlock, cuneiformTileEntity.name, cuneiformTileEntity.desc));
        handleHead.add(new VarInsnNode(ILOAD, 1));
        handleHead.add(new MethodInsnNode(INVOKESTATIC, InternalEditTextHooks, "handleLocalCuneiformEdit",
                "(L" + GuiEditCuneiformBlock + ";L" + TileEntityCuneiformBlock + ";Z)V"));
        if (attemptToGenerateDlaTextCall == null) {
            TransformerUtils.insertToBeginningOfCode(sendTextToServer, handleHead);
        } else {
            sendTextToServer.instructions.insert(attemptToGenerateDlaTextCall, handleHead);
        }
    }

    public static void patchNetServerHandler(ClassNode classNode) {
        // Handle sign server side
        MethodNode handleSignUpdate = TransformerUtils.getMethod(classNode, "handleSignUpdate");
        AbstractInsnNode checkCast = null;
        for (AbstractInsnNode abstractInsnNode : handleSignUpdate.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.CHECKCAST &&
                    ((TypeInsnNode) abstractInsnNode).desc.equals(TileEntitySign)) {
                checkCast = abstractInsnNode;
            }
        }
        Objects.requireNonNull(checkCast, "failed ot find checkCast in handleSignUpdate");
        AbstractInsnNode nextStore = checkCast.getNext();
        if (nextStore.getOpcode() != Opcodes.ASTORE) {
            throw new RuntimeException("Not an ASTORE, got " + nextStore.getOpcode());
        }
        InsnList signInsnList = new InsnList();
        signInsnList.add(new VarInsnNode(ALOAD, 0));
        signInsnList.add(new VarInsnNode(ALOAD, 1));
        signInsnList.add(new VarInsnNode(ALOAD, ((VarInsnNode) nextStore).var));
        signInsnList.add(new MethodInsnNode(INVOKESTATIC, InternalEditTextHooks, "handleRemoteSignEdit",
                "(L" + NetServerHandler + ";L" + Packet130UpdateSign + ";L" + TileEntitySign + ";)Z"));
        LabelNode signEditNotCancelled = new LabelNode();
        signInsnList.add(new JumpInsnNode(Opcodes.IFEQ, signEditNotCancelled));
        signInsnList.add(new InsnNode(Opcodes.RETURN));
        signInsnList.add(signEditNotCancelled);
        handleSignUpdate.instructions.insert(nextStore, signInsnList);
        // Handle cuneiform server side
        MethodNode handleCuneiformBlockUpdate = TransformerUtils.getMethod(classNode, "handleCuneiformBlockUpdate");
        AbstractInsnNode getXPosition = null;
        int varIndexOfCuneiformTileEntity = -1;
        for (AbstractInsnNode abstractInsnNode : handleCuneiformBlockUpdate.instructions) {
            if (varIndexOfCuneiformTileEntity == -1) {
                if (abstractInsnNode.getOpcode() == Opcodes.CHECKCAST &&
                        ((TypeInsnNode) abstractInsnNode).desc.equals(TileEntityCuneiformBlock)) {
                    AbstractInsnNode next = abstractInsnNode.getNext();
                    if (next.getOpcode() != Opcodes.ASTORE) {
                        throw new RuntimeException("Not an ASTORE, got " + nextStore.getOpcode());
                    }
                    varIndexOfCuneiformTileEntity = ((VarInsnNode) next).var;
                }
            } else {
                if (abstractInsnNode.getOpcode() == Opcodes.GETFIELD &&
                        ((FieldInsnNode) abstractInsnNode).name.equals("xPosition")) {
                    getXPosition = abstractInsnNode;
                }
            }
        }
        Objects.requireNonNull(getXPosition, "failed to find xPosition in handleCuneiformBlockUpdate");
        InsnList cuneiformInsnList = new InsnList();
        cuneiformInsnList.add(new VarInsnNode(ALOAD, 0));
        cuneiformInsnList.add(new VarInsnNode(ALOAD, 1));
        cuneiformInsnList.add(new VarInsnNode(ALOAD, varIndexOfCuneiformTileEntity));
        cuneiformInsnList.add(new MethodInsnNode(INVOKESTATIC, InternalEditTextHooks, "handleRemoteCuneiformEdit",
                "(L" + NetServerHandler + ";L" + Packet133UpdateCuneiformBlock + ";L" + TileEntityCuneiformBlock + ";)Z"));
        LabelNode cuneiformEditNotCancelled = new LabelNode();
        cuneiformInsnList.add(new JumpInsnNode(Opcodes.IFEQ, cuneiformEditNotCancelled));
        cuneiformInsnList.add(new InsnNode(Opcodes.RETURN));
        cuneiformInsnList.add(cuneiformEditNotCancelled);
        handleCuneiformBlockUpdate.instructions.insertBefore(getXPosition.getPrevious(), cuneiformInsnList);
        handleCuneiformBlockUpdate.maxStack = Math.max(handleCuneiformBlockUpdate.maxStack, 3);
    }
}
