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
import org.objectweb.asm.tree.*;

final class ClientGameDirectoryPatch extends GamePatch {
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String EntityPlayerSP = "net/minecraft/client/player/EntityPlayerSP";
    private static final String World = "net/minecraft/common/world/World";
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String MinecraftServer = "net/minecraft/server/MinecraftServer";
    private static final String FoxLauncher = "com/fox2code/foxloader/launcher/FoxLauncher";
    private static final String InternalPlayerHooks = "com/fox2code/foxloader/internal/InternalPlayerHooks";

    ClientGameDirectoryPatch() {
        super(Minecraft);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (classNode.name.equals(Minecraft)) {
            // classNode.fields.remove(TransformerUtils.getField(classNode, "minecraftDir"));
            classNode.fields.add(new FieldNode(ACC_PRIVATE, "flNewPlayer", "Z", null, null));
            MethodNode methodNode = TransformerUtils.getMethod(classNode, "getMinecraftDir");
            methodNode.instructions.clear();
            methodNode.localVariables.clear();
            methodNode.exceptions.clear();
            methodNode.instructions.add(new MethodInsnNode(INVOKESTATIC,
                    FoxLauncher, "getGameDir", "()Ljava/io/File;", false));
            methodNode.instructions.add(new InsnNode(ARETURN));
            MethodNode screenshotListener = TransformerUtils.findMethod(classNode, "screenshotListener");
            if (screenshotListener != null) {
                for (AbstractInsnNode abstractInsnNode : screenshotListener.instructions) {
                    if (abstractInsnNode.getOpcode() == GETFIELD) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                        if (fieldInsnNode.name.equals("minecraftDir")) {
                            methodNode.instructions.insert(fieldInsnNode, new MethodInsnNode(INVOKESTATIC,
                                    FoxLauncher, "getGameDir", "()Ljava/io/File;", false));
                            methodNode.instructions.remove(fieldInsnNode);
                            break;
                        }
                    }
                }
            }
            MethodNode startWorld = TransformerUtils.getMethod(classNode, "startWorld");
            InsnList startWorldPre = new InsnList();
            startWorldPre.add(new VarInsnNode(ALOAD, 0));
            startWorldPre.add(new InsnNode(ICONST_1));
            startWorldPre.add(new FieldInsnNode(PUTFIELD, Minecraft, "flNewPlayer", "Z"));
            AbstractInsnNode gc = null;
            for (AbstractInsnNode abstractInsnNode : startWorld.instructions) {
                if (abstractInsnNode.getOpcode() == INVOKESTATIC) {
                    gc = abstractInsnNode;
                    break;
                }
            }
            startWorld.instructions.insertBefore(gc, startWorldPre);
            InsnList startWorldPost = new InsnList();
            startWorldPost.add(new VarInsnNode(ALOAD, 0));
            startWorldPost.add(new InsnNode(ICONST_0));
            startWorldPost.add(new FieldInsnNode(PUTFIELD, Minecraft, "flNewPlayer", "Z"));
            TransformerUtils.insertToEndOfCode(startWorld, startWorldPost);
            MethodNode changeWorld = TransformerUtils.getMethod(classNode, "changeWorld",
                    "(L" + World + ";Ljava/lang/String;L" + EntityPlayer + ";)V");
            AbstractInsnNode setPlayer1 = null, setPlayer2 = null, preparePlayerToSpawn = null;
            InsnList changeWorldPre = new InsnList();
            changeWorldPre.add(new VarInsnNode(ALOAD, 1));
            LabelNode changeWorldPreEnd = new LabelNode();
            changeWorldPre.add(new JumpInsnNode(IFNONNULL, changeWorldPreEnd));
            changeWorldPre.add(new VarInsnNode(ALOAD, 0));
            changeWorldPre.add(new FieldInsnNode(GETFIELD, Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
            changeWorldPre.add(new JumpInsnNode(IFNULL, changeWorldPreEnd));
            changeWorldPre.add(new InsnNode(ACONST_NULL));
            changeWorldPre.add(new VarInsnNode(ALOAD, 0));
            changeWorldPre.add(new FieldInsnNode(GETFIELD, Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
            changeWorldPre.add(new MethodInsnNode(INVOKESTATIC, InternalPlayerHooks,
                    "sendPlayerLeaveEvent", "(L" + MinecraftServer + ";L" + EntityPlayer + ";)V", false));
            changeWorldPre.add(changeWorldPreEnd);
            TransformerUtils.insertToBeginningOfCode(changeWorld, changeWorldPre);
            for (AbstractInsnNode abstractInsnNode : changeWorld.instructions) {
                final int opcode = abstractInsnNode.getOpcode();
                if (opcode == PUTFIELD && ((FieldInsnNode) abstractInsnNode).name.equals("thePlayer")) {
                    if (setPlayer1 == null) {
                        setPlayer1 = abstractInsnNode;
                    } else if (setPlayer2 == null) {
                        setPlayer2 = abstractInsnNode;
                    }
                } else if (opcode == INVOKEVIRTUAL && setPlayer2 != null &&
                        ((MethodInsnNode) abstractInsnNode).desc.endsWith("V")) {
                    preparePlayerToSpawn = abstractInsnNode;
                    break;
                }
            }
            for (AbstractInsnNode setPlayer : new AbstractInsnNode[]{setPlayer1, preparePlayerToSpawn}) {
                InsnList changeWorldSetPlayer = new InsnList();
                changeWorldSetPlayer.add(new VarInsnNode(ALOAD, 0));
                changeWorldSetPlayer.add(new FieldInsnNode(GETFIELD, Minecraft, "flNewPlayer", "Z"));
                LabelNode notNewPlayer = new LabelNode();
                changeWorldSetPlayer.add(new JumpInsnNode(IFEQ, notNewPlayer));
                if (setPlayer != preparePlayerToSpawn) {
                    changeWorldSetPlayer.add(new VarInsnNode(ALOAD, 0));
                    changeWorldSetPlayer.add(new FieldInsnNode(GETFIELD, Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
                    changeWorldSetPlayer.add(new JumpInsnNode(IFNULL, notNewPlayer));
                }
                changeWorldSetPlayer.add(new VarInsnNode(ALOAD, 0));
                changeWorldSetPlayer.add(new InsnNode(ICONST_0));
                changeWorldSetPlayer.add(new FieldInsnNode(PUTFIELD, Minecraft, "flNewPlayer", "Z"));
                changeWorldSetPlayer.add(new InsnNode(ACONST_NULL));
                changeWorldSetPlayer.add(new VarInsnNode(ALOAD, 0));
                changeWorldSetPlayer.add(new FieldInsnNode(GETFIELD, Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
                changeWorldSetPlayer.add(new MethodInsnNode(INVOKESTATIC, InternalPlayerHooks,
                        "sendPlayerJoinEvent", "(L" + MinecraftServer + ";L" + EntityPlayer + ";)V", false));
                changeWorldSetPlayer.add(notNewPlayer);
                changeWorld.instructions.insert(setPlayer, changeWorldSetPlayer);
            }
            MethodNode respawnPlayer = TransformerUtils.getMethod(classNode, "respawn");
            InsnList respawnPlayerAppend = new InsnList();
            respawnPlayerAppend.add(new VarInsnNode(ALOAD, 0));
            respawnPlayerAppend.add(new FieldInsnNode(GETFIELD,
                    Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
            respawnPlayerAppend.add(new MethodInsnNode(INVOKESTATIC, InternalPlayerHooks,
                    "sendPlayerRespawnEvent", "(L" + EntityPlayer + ";)V"));
            TransformerUtils.insertToEndOfCode(respawnPlayer, respawnPlayerAppend);
        }
        return classNode;
    }
}
