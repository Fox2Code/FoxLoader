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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Objects;

final class NetworkConnectionPatch extends GamePatch {
    private static final String IntOpenHashSet = "it/unimi/dsi/fastutil/ints/IntOpenHashSet";
    private static final String NetClientHandler = "net/minecraft/client/networking/NetClientHandler";
    private static final String NetServerHandler = "net/minecraft/server/networking/NetServerHandler";
    private static final String NetLoginHandler = "net/minecraft/server/networking/NetLoginHandler";
    private static final String NetworkManager = "net/minecraft/common/networking/NetworkManager";
    private static final String NetHandler = "net/minecraft/common/networking/NetHandler";
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String EntityPlayerMP = "net/minecraft/server/entity/player/EntityPlayerMP";
    private static final String EntityPlayerSP = "net/minecraft/client/player/EntityPlayerSP";
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String MinecraftServer = "net/minecraft/server/MinecraftServer";
    private static final String ServerConfigurationManager = "net/minecraft/server/util/ServerConfigurationManager";
    private static final String Packet = "net/minecraft/common/networking/Packet";
    private static final String Packet2ClientProtocol = "net/minecraft/common/networking/Packet2ClientProtocol";
    private static final String Packet250PluginMessage = "net/minecraft/common/networking/Packet250PluginMessage";
    private static final String ModContainer = "com/fox2code/foxloader/loader/ModContainer";
    private static final String ModLoaderInit = "com/fox2code/foxloader/loader/ModLoaderInit";
    private static final String EntityClientPlayerMP = "net/minecraft/client/player/EntityClientPlayerMP";
    private static final String ConnectionType = "com/fox2code/foxloader/network/ConnectionType";
    private static final String InternalNetworkHooks = "com/fox2code/foxloader/internal/InternalNetworkHooks";
    private static final String InternalPlayerHooks = "com/fox2code/foxloader/internal/InternalPlayerHooks";
    private static final String FL_NETWORK_HEADER = "\0fl";
    private static final String FB_NETWORK_HEADER = "\0fb";
    private static final String FLB_NETWORK_HEADER = "\0flb";

    NetworkConnectionPatch() {
        super(new String[]{NetClientHandler, NetServerHandler, NetLoginHandler,
                NetworkManager, NetHandler, Packet, Packet2ClientProtocol, ServerConfigurationManager,
                EntityPlayer, EntityPlayerMP, EntityPlayerSP, EntityClientPlayerMP});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case EntityPlayer:
                return patchEntityPlayer(classNode, "NONE", "");
            case EntityPlayerMP:
                return patchEntityPlayer(classNode, "SERVER_ONLY", NetServerHandler);
            case EntityPlayerSP:
                return patchEntityPlayer(classNode, "SINGLE_PLAYER", null);
            case EntityClientPlayerMP:
                return patchEntityPlayer(classNode, "CLIENT_ONLY", NetClientHandler);
            case NetworkManager:
                return transformNetworkManager(classNode);
            case NetHandler:
                return transformNetHandler(classNode);
            case Packet:
                return transformPacket(classNode);
            case Packet2ClientProtocol:
                return transformPacket2ClientProtocol(classNode);
            case NetClientHandler:
                return transformNetHandlers(classNode, true, false, false);
            case NetLoginHandler:
                return transformNetHandlers(classNode, false, true, false);
            case NetServerHandler:
                return transformNetHandlers(classNode, false, false, true);
        }
        return classNode;
    }

    private static ClassNode patchEntityPlayer(ClassNode classNode, String enumConstant, String netHandler) {
        MethodNode getConnectionType = new MethodNode(ACC_PUBLIC,
                "getConnectionType", "()L" + ConnectionType + ";", null, null);
        getConnectionType.instructions.add(new FieldInsnNode(GETSTATIC,
                ConnectionType, enumConstant, "L" + ConnectionType + ";"));
        getConnectionType.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getConnectionType);
        if (netHandler == null) return classNode;
        FieldNode fieldNode = netHandler.isEmpty() ? null :
                TransformerUtils.getFieldDesc(classNode, "L" + netHandler + ";");
        if (netHandler.isEmpty() || NetServerHandler.equals(netHandler)) {
            MethodNode hasFoxLoader = new MethodNode(ACC_PUBLIC, "hasFoxLoader", "()Z", null, null);
            if (fieldNode == null) {
                hasFoxLoader.instructions.add(new InsnNode(ICONST_1));
            } else {
                hasFoxLoader.instructions.add(new VarInsnNode(ALOAD, 0));
                hasFoxLoader.instructions.add(new FieldInsnNode(
                        GETFIELD, classNode.name, fieldNode.name, fieldNode.desc));
                hasFoxLoader.instructions.add(new MethodInsnNode(
                        INVOKEVIRTUAL, netHandler, "getNetworkManager", "()L" + NetworkManager + ";"));
                hasFoxLoader.instructions.add(new MethodInsnNode(
                        INVOKEVIRTUAL, NetworkManager, "hasFoxLoader", "()Z"));
            }
            hasFoxLoader.instructions.add(new InsnNode(IRETURN));
            classNode.methods.add(hasFoxLoader);
        }
        MethodNode sendNetworkData = new MethodNode(ACC_PUBLIC,
                "sendNetworkData", "(L" + ModContainer + ";[B)V", null, null);
        if (fieldNode != null) {
            sendNetworkData.instructions.add(new VarInsnNode(ALOAD, 0));
            sendNetworkData.instructions.add(new FieldInsnNode(
                    GETFIELD, classNode.name, fieldNode.name, fieldNode.desc));
            sendNetworkData.instructions.add(new MethodInsnNode(
                    INVOKEVIRTUAL, netHandler, "getNetworkManager", "()L" + NetworkManager + ";"));
            sendNetworkData.instructions.add(new MethodInsnNode(
                    INVOKEVIRTUAL, NetworkManager, "hasFoxLoader", "()Z"));
            LabelNode remoteDoNotHaveFoxLoader = new LabelNode();
            sendNetworkData.instructions.add(new JumpInsnNode(IFEQ, remoteDoNotHaveFoxLoader));
            sendNetworkData.instructions.add(new VarInsnNode(ALOAD, 0));
            sendNetworkData.instructions.add(new FieldInsnNode(
                    GETFIELD, classNode.name, fieldNode.name, fieldNode.desc));
            sendNetworkData.instructions.add(new MethodInsnNode(
                    INVOKEVIRTUAL, netHandler, "getNetworkManager", "()L" + NetworkManager + ";"));
            sendNetworkData.instructions.add(new TypeInsnNode(NEW, Packet250PluginMessage));
            sendNetworkData.instructions.add(new InsnNode(DUP));
            sendNetworkData.instructions.add(new VarInsnNode(ALOAD, 1));
            sendNetworkData.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                    ModContainer, "getModId", "()L" + String + ";", false));
            sendNetworkData.instructions.add(new VarInsnNode(ALOAD, 2));
            sendNetworkData.instructions.add(new MethodInsnNode(
                    INVOKESPECIAL, Packet250PluginMessage, "<init>", "(L" + String + ";[B)V"));
            sendNetworkData.instructions.add(new MethodInsnNode(
                    INVOKEVIRTUAL, NetworkManager, "addToSendQueue", "(L" + Packet + ";)V"));
            sendNetworkData.instructions.add(remoteDoNotHaveFoxLoader);
        }
        sendNetworkData.instructions.add(new InsnNode(RETURN));
        TransformerUtils.setThisParameterName(classNode, sendNetworkData);
        TransformerUtils.setParameterName(sendNetworkData, 1, "modContainer");
        TransformerUtils.setParameterName(sendNetworkData, 2, "data");
        classNode.methods.add(sendNetworkData);
        return classNode;
    }

    private static ClassNode transformNetworkManager(ClassNode classNode) {
        classNode.access |= Opcodes.ACC_FINAL;
        classNode.fields.add(new FieldNode(0, "hasFoxLoader", "Z", null, false));
        classNode.fields.add(new FieldNode(ACC_PUBLIC, "flHelloHandler", "Ljava/lang/Object;", null, null));
        MethodNode getNetHandler = new MethodNode(ACC_PUBLIC,
                "getNetHandler", "()L" + NetHandler + ";", null, null);
        getNetHandler.instructions.add(new VarInsnNode(ALOAD, 0));
        getNetHandler.instructions.add(new FieldInsnNode(GETFIELD,
                NetworkManager, "netHandler", "L" + NetHandler + ";"));
        getNetHandler.instructions.add(new InsnNode(ARETURN));
        TransformerUtils.addMethodBefore(classNode, "setNetHandler", getNetHandler);
        MethodNode hasFoxLoader = new MethodNode(ACC_PUBLIC,
                "hasFoxLoader", "()Z", null, null);
        hasFoxLoader.instructions.add(new VarInsnNode(ALOAD, 0));
        hasFoxLoader.instructions.add(new FieldInsnNode(GETFIELD,
                NetworkManager, "hasFoxLoader", "Z"));
        hasFoxLoader.instructions.add(new InsnNode(IRETURN));
        classNode.methods.add(hasFoxLoader);
        MethodNode getEntityPlayer = new MethodNode(ACC_PUBLIC,
                "getEntityPlayer", "()L" + EntityPlayer + ";", null, null);
        getEntityPlayer.instructions.add(new VarInsnNode(ALOAD, 0));
        getEntityPlayer.instructions.add(new FieldInsnNode(GETFIELD,
                NetworkManager, "netHandler", "L" + NetHandler + ";"));
        getEntityPlayer.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                NetHandler, "getEntityPlayer", "()L" + EntityPlayer + ";"));
        getEntityPlayer.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getEntityPlayer);
        MethodNode onNetworkError = TransformerUtils.getMethod(classNode, "onNetworkError");
        AbstractInsnNode firstCodeInsn = TransformerUtils.nextCodeInsn(onNetworkError.instructions.getFirst());
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ALOAD, 1));
        insnList.add(new MethodInsnNode(INVOKESTATIC, InternalNetworkHooks,
                "onNetworkError", "(L" + NetworkManager + ";Ljava/lang/Exception;)V"));
        onNetworkError.instructions.insertBefore(firstCodeInsn, insnList);
        return classNode;
    }

    private static ClassNode transformNetHandler(ClassNode classNode) {
        classNode.methods.add(new MethodNode(ACC_PUBLIC | ACC_ABSTRACT,
                "getNetworkManager", "()L" + NetworkManager + ";", null, null));
        classNode.methods.add(new MethodNode(ACC_PUBLIC | ACC_ABSTRACT,
                "getEntityPlayer", "()L" + EntityPlayer + ";", null, null));
        MethodNode markHasFoxLoader = new MethodNode(ACC_PROTECTED | ACC_FINAL,
                "markHasFoxLoader", "()V", null, null);
        markHasFoxLoader.instructions.add(new VarInsnNode(ALOAD, 0));
        markHasFoxLoader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                NetHandler, "getNetworkManager", "()L" + NetworkManager + ";", false));
        markHasFoxLoader.instructions.add(new InsnNode(ICONST_1));
        markHasFoxLoader.instructions.add(new FieldInsnNode(PUTFIELD,
                NetworkManager, "hasFoxLoader", "Z"));
        markHasFoxLoader.instructions.add(new InsnNode(RETURN));
        classNode.methods.add(markHasFoxLoader);
        return classNode;
    }

    private static ClassNode transformPacket(ClassNode classNode) {
        TransformerUtils.getMethod(classNode, "readNbtTagCompound").access = ACC_PUBLIC | ACC_STATIC;
        TransformerUtils.getMethod(classNode, "writeNbtTagCompound").access = ACC_PUBLIC | ACC_STATIC;
        TransformerUtils.addFieldAfter(classNode, "loginPacketIdList",
                new FieldNode(ACC_PRIVATE | ACC_FINAL | ACC_STATIC,
                        "flLoginPacketIdList", "L" + IntOpenHashSet + ";", null, null));
        MethodNode clInit = TransformerUtils.getMethod(classNode, "<clinit>");
        boolean patched = false;
        for (AbstractInsnNode abstractInsnNode : clInit.instructions) {
            if (abstractInsnNode.getOpcode() == PUTSTATIC) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                if (fieldInsnNode.name.equals("loginPacketIdList")) {
                    AbstractInsnNode beginning = TransformerUtils.previousNonCodeInsn(fieldInsnNode);
                    if (beginning.getType() == AbstractInsnNode.LINE) beginning = beginning.getPrevious();
                    InsnList newCode = TransformerUtils.copyCodeUntil(beginning, PUTSTATIC);
                    for (AbstractInsnNode newInsn : newCode) {
                        if (newInsn.getType() == AbstractInsnNode.LINE) {
                            ((LineNumberNode) newInsn).line++;
                        } else if (newInsn.getOpcode() == PUTSTATIC) {
                            ((FieldInsnNode) newInsn).name = "flLoginPacketIdList";
                        }
                    }
                    clInit.instructions.insert(fieldInsnNode, newCode);
                    patched = true;
                    break;
                }
            }
        }
        AbstractInsnNode lastInsn = TransformerUtils.previousCodeInsn(clInit.instructions.getLast());
        InsnList flAllowPluginDataOnFLLogin = new InsnList();
        flAllowPluginDataOnFLLogin.add(new FieldInsnNode(GETSTATIC,
                Packet, "flLoginPacketIdList", "L" + IntOpenHashSet + ";"));
        flAllowPluginDataOnFLLogin.add(TransformerUtils.getNumberInsn(250));
        flAllowPluginDataOnFLLogin.add(new MethodInsnNode(INVOKEVIRTUAL,
                IntOpenHashSet, "add", "(I)Z", false));
        flAllowPluginDataOnFLLogin.add(new InsnNode(POP));
        clInit.instructions.insert(lastInsn, flAllowPluginDataOnFLLogin);
        if (!patched) throw new IllegalStateException("Patch failed");
        patched = false;
        MethodNode addIdClassMapping = TransformerUtils.getMethod(classNode, "addIdClassMapping");
        for (AbstractInsnNode abstractInsnNode : addIdClassMapping.instructions) {
            if (abstractInsnNode.getOpcode() == GETSTATIC) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                if (fieldInsnNode.name.equals("loginPacketIdList")) {
                    InsnList newCode = TransformerUtils.copyCodeUntil(fieldInsnNode, POP);
                    for (AbstractInsnNode newInsn : newCode) {
                        if (newInsn.getOpcode() == GETSTATIC) {
                            ((FieldInsnNode) newInsn).name = "flLoginPacketIdList";
                        }
                    }
                    addIdClassMapping.instructions.insertBefore(
                            TransformerUtils.nextNonCodeInsn(fieldInsnNode),
                            newCode);
                    patched = true;
                    break;
                }
            }
        }
        if (!patched) throw new IllegalStateException("Patch failed");
        MethodNode readPacket = TransformerUtils.getMethod(classNode, "readPacket");
        TableSwitchInsnNode tableSwitchInsnNode = null;
        LabelNode gotoLabel = null;
        for (AbstractInsnNode abstractInsnNode : readPacket.instructions) {
            if (tableSwitchInsnNode == null) {
                if (abstractInsnNode.getOpcode() == TABLESWITCH) {
                    tableSwitchInsnNode = (TableSwitchInsnNode) abstractInsnNode;
                }
            } else if (abstractInsnNode.getOpcode() == GOTO) {
                gotoLabel = ((JumpInsnNode) abstractInsnNode).label;
                break;
            }
        }
        if (tableSwitchInsnNode == null) throw new IllegalStateException("Patch failed");
        if (gotoLabel == null) throw new IllegalStateException("Patch failed");
        LabelNode lastSwitchLabel = tableSwitchInsnNode.labels.get(tableSwitchInsnNode.labels.size() - 1);
        tableSwitchInsnNode.max++;
        LabelNode flLoginBranchLabel = new LabelNode();
        tableSwitchInsnNode.labels.add(flLoginBranchLabel);
        AbstractInsnNode copyStart = TransformerUtils.nextCodeInsn(lastSwitchLabel);
        InsnList flLoginBranchCode = TransformerUtils.copyCodeUntil(copyStart, ASTORE);
        for (AbstractInsnNode newInsn : flLoginBranchCode) {
            if (newInsn.getOpcode() == GETSTATIC) {
                ((FieldInsnNode) newInsn).name = "flLoginPacketIdList";
            }
        }
        flLoginBranchCode.insert(flLoginBranchLabel);
        flLoginBranchCode.insertBefore(flLoginBranchLabel, new JumpInsnNode(GOTO, gotoLabel));
        readPacket.instructions.insert(TransformerUtils.previousCodeInsn(tableSwitchInsnNode.dflt), flLoginBranchCode);
        return classNode;
    }

    private static ClassNode transformPacket2ClientProtocol(ClassNode classNode) {
        MethodNode cst = TransformerUtils.getMethod(classNode,
                "<init>", "(ILjava/lang/String;Ljava/lang/String;I)V");
        for (AbstractInsnNode abstractInsnNode : cst.instructions) {
            if (abstractInsnNode.getOpcode() == ALOAD) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                if (varInsnNode.var == 3) {
                    InsnList insnList = new InsnList();
                    insnList.add(new LdcInsnNode(FL_NETWORK_HEADER));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                            "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;"));
                    cst.instructions.insert(varInsnNode, insnList);
                    break;
                }
            }
        }
        MethodNode getServerHost = new MethodNode(ACC_PUBLIC,
                "getServerHost", "()Ljava/lang/String;", null, null);
        getServerHost.instructions.add(new VarInsnNode(ALOAD, 0));
        getServerHost.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        getServerHost.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getServerHost);
        MethodNode consumeFoxLoaderHeader = new MethodNode(ACC_PUBLIC,
                "consumeFoxLoaderHeader", "()Z", null, null);
        // Check FL Header
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new LdcInsnNode(FL_NETWORK_HEADER));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        LabelNode hasFoxLoaderHeader = new LabelNode();
        consumeFoxLoaderHeader.instructions.add(new JumpInsnNode(IFNE, hasFoxLoaderHeader));
        // Check FB Header
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new LdcInsnNode(FB_NETWORK_HEADER));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        LabelNode hasFoxBucketHeader = new LabelNode();
        consumeFoxLoaderHeader.instructions.add(new JumpInsnNode(IFNE, hasFoxBucketHeader));
        // Check FLB Header
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new LdcInsnNode(FLB_NETWORK_HEADER));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false));
        LabelNode hasFoxLoaderBucketHeader = new LabelNode();
        consumeFoxLoaderHeader.instructions.add(new JumpInsnNode(IFNE, hasFoxLoaderBucketHeader));
        // No Header Match
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_0));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(IRETURN));
        // Handle FL Header
        consumeFoxLoaderHeader.instructions.add(hasFoxLoaderHeader);
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_0));
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "length", "()I", false));
        consumeFoxLoaderHeader.instructions.add(
                TransformerUtils.getNumberInsn(FL_NETWORK_HEADER.length()));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ISUB));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "substring", "(II)Ljava/lang/String;", false));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(PUTFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_1));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(IRETURN));
        // Handle FB Header
        consumeFoxLoaderHeader.instructions.add(hasFoxBucketHeader);
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_0));
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "length", "()I", false));
        consumeFoxLoaderHeader.instructions.add(
                TransformerUtils.getNumberInsn(FB_NETWORK_HEADER.length()));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ISUB));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "substring", "(II)Ljava/lang/String;", false));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(PUTFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_0));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(IRETURN));
        // Handle FLB Header
        consumeFoxLoaderHeader.instructions.add(hasFoxLoaderBucketHeader);
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_0));
        consumeFoxLoaderHeader.instructions.add(new VarInsnNode(ALOAD, 0));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(GETFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "length", "()I", false));
        consumeFoxLoaderHeader.instructions.add(
                TransformerUtils.getNumberInsn(FLB_NETWORK_HEADER.length()));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ISUB));
        consumeFoxLoaderHeader.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/String", "substring", "(II)Ljava/lang/String;", false));
        consumeFoxLoaderHeader.instructions.add(new FieldInsnNode(PUTFIELD,
                Packet2ClientProtocol, "serverHost", "Ljava/lang/String;"));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(ICONST_1));
        consumeFoxLoaderHeader.instructions.add(new InsnNode(IRETURN));
        classNode.methods.add(consumeFoxLoaderHeader);
        return classNode;
    }

    public static ClassNode transformNetHandlers(ClassNode classNode, boolean client, boolean login, boolean server) {
        String owner = classNode.name;
        FieldNode networkManagerField =
                TransformerUtils.getFieldDesc(classNode, "L" + NetworkManager + ";");
        MethodNode getNetworkManager = new MethodNode(ACC_PUBLIC,
                "getNetworkManager", "()L" + NetworkManager + ";", null, null);
        getNetworkManager.instructions.add(new VarInsnNode(ALOAD, 0));
        getNetworkManager.instructions.add(new FieldInsnNode(GETFIELD, owner,
                networkManagerField.name, "L" + NetworkManager + ";"));
        getNetworkManager.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getNetworkManager);
        MethodNode getEntityPlayer = new MethodNode(ACC_PUBLIC,
                "getEntityPlayer", "()L" + EntityPlayer + ";", null, null);
        if (client) {
            getEntityPlayer.instructions.add(new VarInsnNode(ALOAD, 0));
            getEntityPlayer.instructions.add(new FieldInsnNode(
                    GETFIELD, NetClientHandler, "mc", "L" + Minecraft + ";"));
            getEntityPlayer.instructions.add(new FieldInsnNode(
                    GETFIELD, Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
        } else if (server) {
            getEntityPlayer.instructions.add(new VarInsnNode(ALOAD, 0));
            getEntityPlayer.instructions.add(new FieldInsnNode(
                    GETFIELD, NetServerHandler, "playerEntity", "L" + EntityPlayerMP + ";"));
        } else {
            getEntityPlayer.instructions.add(new InsnNode(ACONST_NULL));
        }
        getEntityPlayer.instructions.add(new InsnNode(ARETURN));
        classNode.methods.add(getEntityPlayer);
        MethodNode handlePluginMessage = TransformerUtils.findMethod(classNode,
                "handlePluginMessage", "(L" + Packet250PluginMessage + ";)V");
        if (handlePluginMessage != null) {
            classNode.methods.remove(handlePluginMessage);
        }
        classNode.methods.add(handlePluginMessage = new MethodNode(ACC_PUBLIC,
                "handlePluginMessage", "(L" + Packet250PluginMessage + ";)V", null, null));
        LabelNode handlePluginMessageStart = new LabelNode();
        LabelNode handlePluginMessageEnd = new LabelNode();
        handlePluginMessage.localVariables.add(new LocalVariableNode("this",
                "L" + owner + ";", null, handlePluginMessageStart, handlePluginMessageEnd, 0));
        handlePluginMessage.localVariables.add(new LocalVariableNode("pluginMessage",
                "L" + Packet250PluginMessage + ";", null, handlePluginMessageStart, handlePluginMessageEnd, 1));
        LabelNode modVariableStart = new LabelNode();
        handlePluginMessage.localVariables.add(new LocalVariableNode("mod",
                "L" + ModContainer + ";", null, modVariableStart, handlePluginMessageEnd, 2));
        InsnList handlePluginMessageInstructions = handlePluginMessage.instructions;
        handlePluginMessageInstructions.add(handlePluginMessageStart);
        if (client) {
            handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 1));
            handlePluginMessageInstructions.add(new FieldInsnNode(GETFIELD,
                    Packet250PluginMessage, "channel", "Ljava/lang/String;"));
            handlePluginMessageInstructions.add(new LdcInsnNode("foxloader"));
            handlePluginMessageInstructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                    "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
            LabelNode notFoxLoaderMessage = new LabelNode();
            handlePluginMessageInstructions.add(new JumpInsnNode(IFEQ, notFoxLoaderMessage));
            handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 0));
            handlePluginMessageInstructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                    NetClientHandler, "markHasFoxLoader", "()V"));
            handlePluginMessageInstructions.add(notFoxLoaderMessage);
        }
        handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 0));
        handlePluginMessageInstructions.add(new FieldInsnNode(GETFIELD,
                owner, networkManagerField.name, "L" + NetworkManager + ";"));
        handlePluginMessageInstructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                NetworkManager, "hasFoxLoader", "()Z", false));
        LabelNode hasFoxLoaderJmp = new LabelNode();
        handlePluginMessageInstructions.add(new JumpInsnNode(IFNE, hasFoxLoaderJmp));
        if (login) {
            handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 0));
            handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 1));
            handlePluginMessageInstructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                    NetLoginHandler, "unexpectedPacket", "(L" + Packet + ";)V", false));
        }
        handlePluginMessageInstructions.add(new InsnNode(RETURN));
        handlePluginMessageInstructions.add(hasFoxLoaderJmp);
        handlePluginMessageInstructions.add(modVariableStart);
        handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 1));
        handlePluginMessageInstructions.add(new FieldInsnNode(GETFIELD,
                Packet250PluginMessage, "channel", "Ljava/lang/String;"));
        handlePluginMessageInstructions.add(new MethodInsnNode(INVOKESTATIC,
                ModLoaderInit, "getModContainer", "(Ljava/lang/String;)L" + ModContainer + ";"));
        handlePluginMessageInstructions.add(new VarInsnNode(ASTORE, 2));
        handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 2));
        LabelNode noModObject = new LabelNode();
        handlePluginMessageInstructions.add(new JumpInsnNode(IFNULL, noModObject));
        handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 2));
        handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 0));
        handlePluginMessageInstructions.add(new FieldInsnNode(GETFIELD,
                owner, networkManagerField.name, "L" + NetworkManager + ";"));
        handlePluginMessageInstructions.add(new VarInsnNode(ALOAD, 1));
        handlePluginMessageInstructions.add(new FieldInsnNode(GETFIELD,
                Packet250PluginMessage, "data", "[B"));
        handlePluginMessageInstructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                ModContainer, client ? "onReceiveDataFromServer" : "onReceiveDataFromClient",
                "(L" + NetworkManager + ";[B)V"));
        handlePluginMessageInstructions.add(noModObject);
        handlePluginMessageInstructions.add(new InsnNode(RETURN));
        handlePluginMessageInstructions.add(handlePluginMessageEnd);
        if (server) {
            for (String disconnectMethod : new String[]{"kickPlayer", "handleErrorMessage"}) {
                MethodNode disconnectMethodNode = TransformerUtils.getMethod(classNode, disconnectMethod);
                // Get non code entry before and after the left message send code
                AbstractInsnNode lastCodeLineLabel = null, start = null, end = null;
                for (AbstractInsnNode abstractInsnNode : disconnectMethodNode.instructions) {
                    if (abstractInsnNode.getType() == AbstractInsnNode.LINE) {
                        if (start != null) {
                            end = TransformerUtils.previousCodeInsn(abstractInsnNode).getNext();
                            break;
                        }
                        lastCodeLineLabel = abstractInsnNode;
                        continue;
                    } else if (abstractInsnNode.getOpcode() == LDC && // TODO: Use code check instead?
                            ((LdcInsnNode)abstractInsnNode).cst.toString().contains("left")) {
                        start = TransformerUtils.nextCodeInsn(lastCodeLineLabel).getPrevious();
                    }
                }
                Objects.requireNonNull(end, "end");
                // Remove sending message
                TransformerUtils.removeInstructionsInRange(
                        disconnectMethodNode.instructions, start, end);
                // Add FoxLoader handling
                InsnList foxLoaderJoinMessage = new InsnList();
                foxLoaderJoinMessage.add(new VarInsnNode(ALOAD, 0));
                foxLoaderJoinMessage.add(new FieldInsnNode(GETFIELD, NetServerHandler,
                        "mcServer", "L" + MinecraftServer + ";"));
                foxLoaderJoinMessage.add(new VarInsnNode(ALOAD, 0));
                foxLoaderJoinMessage.add(new FieldInsnNode(GETFIELD, NetServerHandler,
                        "playerEntity", "L" + EntityPlayerMP + ";"));
                foxLoaderJoinMessage.add(new MethodInsnNode(INVOKESTATIC, InternalPlayerHooks,
                        "sendPlayerLeaveEvent", "(L" + MinecraftServer + ";L" + EntityPlayer + ";)V", false));
                disconnectMethodNode.instructions.insert(start, foxLoaderJoinMessage);
            }
        } else if (login) {
            MethodNode handleClientProtocol = TransformerUtils.getMethod(classNode, "handleClientProtocol");
            boolean afterCst = false, didPatch = false;
            for (AbstractInsnNode abstractInsnNode : handleClientProtocol.instructions) {
                if (abstractInsnNode.getOpcode() == LDC) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                    if ("Invalid username!".equals(ldcInsnNode.cst)) afterCst = true;
                } else if (abstractInsnNode.getOpcode() == ALOAD && afterCst) {
                    InsnList insnList = new InsnList();
                    insnList.add(new VarInsnNode(ALOAD, 1));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                            Packet2ClientProtocol, "consumeFoxLoaderHeader", "()Z", false));
                    LabelNode noFoxLoaderHeader = new LabelNode();
                    insnList.add(new JumpInsnNode(IFEQ, noFoxLoaderHeader));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                            NetLoginHandler, "markHasFoxLoader", "()V", false));
                    insnList.add(new VarInsnNode(ALOAD, 0));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                            NetLoginHandler, "getNetworkManager", "()L" + NetworkManager + ";", false));
                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                            InternalNetworkHooks, "sendServerHello", "(L" + NetworkManager + ";)V", false));
                    insnList.add(noFoxLoaderHeader);
                    handleClientProtocol.instructions.insertBefore(abstractInsnNode, insnList);
                    didPatch = true;
                    break;
                }
            }
            if (!didPatch) {
                throw new RuntimeException("Failed to apply handleClientProtocol patch");
            }
            MethodNode initializePlayerConnection = TransformerUtils.getMethod(classNode, "initializePlayerConnection");
            InsnList initializePlayerConnectionPre = new InsnList();
            initializePlayerConnectionPre.add(new VarInsnNode(ALOAD, 0));
            initializePlayerConnectionPre.add(new FieldInsnNode(GETFIELD,
                    NetLoginHandler, networkManagerField.name, "L" + NetworkManager + ";"));
            initializePlayerConnectionPre.add(new VarInsnNode(ALOAD, 0));
            initializePlayerConnectionPre.add(new FieldInsnNode(GETFIELD,
                    NetLoginHandler, "username", "Ljava/lang/String;"));
            initializePlayerConnectionPre.add(new MethodInsnNode(INVOKESTATIC,
                    InternalNetworkHooks, "sendPlayerConnectEvent",
                    "(L" + NetworkManager + ";Ljava/lang/String;)Z"));
            LabelNode everythingFine = new LabelNode();
            initializePlayerConnectionPre.add(new JumpInsnNode(IFEQ, everythingFine));
            initializePlayerConnectionPre.add(new InsnNode(RETURN));
            initializePlayerConnectionPre.add(everythingFine);
            TransformerUtils.insertToBeginningOfCode(initializePlayerConnection, initializePlayerConnectionPre);
            InsnList initializePlayerConnectionPost = new InsnList();
            initializePlayerConnectionPost.add(new VarInsnNode(ALOAD, 0));
            initializePlayerConnectionPost.add(new FieldInsnNode(GETFIELD,
                    NetLoginHandler, "mcServer", "L" + MinecraftServer + ";"));
            initializePlayerConnectionPost.add(new VarInsnNode(ALOAD, 1));
            initializePlayerConnectionPost.add(new MethodInsnNode(INVOKESTATIC, InternalPlayerHooks,
                    "sendPlayerJoinEvent", "(L" + MinecraftServer + ";L" + EntityPlayer + ";)V", false));
            TransformerUtils.insertToEndOfCode(initializePlayerConnection, initializePlayerConnectionPost);
            MethodNode getHandlerProtocol = TransformerUtils.getMethod(classNode, "getHandlerProtocol");
            AbstractInsnNode iconst2 = null;
            for (AbstractInsnNode abstractInsnNode : getHandlerProtocol.instructions) {
                if (abstractInsnNode.getOpcode() == ICONST_2) {
                    iconst2 = abstractInsnNode;
                    break;
                }
            }
            if (iconst2 == null) throw new RuntimeException("Patch failed");
            InsnList protocol2or3ifFL = new InsnList();
            LabelNode noFL = new LabelNode();
            LabelNode end = new LabelNode();
            protocol2or3ifFL.add(new VarInsnNode(ALOAD, 0));
            protocol2or3ifFL.add(new FieldInsnNode(GETFIELD, NetLoginHandler,
                    networkManagerField.name, "L" + NetworkManager + ";"));
            protocol2or3ifFL.add(new JumpInsnNode(IFNULL, noFL));
            protocol2or3ifFL.add(new VarInsnNode(ALOAD, 0));
            protocol2or3ifFL.add(new FieldInsnNode(GETFIELD, NetLoginHandler,
                    networkManagerField.name, "L" + NetworkManager + ";"));
            protocol2or3ifFL.add(new MethodInsnNode(INVOKEVIRTUAL,
                    NetworkManager, "hasFoxLoader", "()Z", false));
            protocol2or3ifFL.add(new JumpInsnNode(IFEQ, noFL));
            protocol2or3ifFL.add(new InsnNode(ICONST_3));
            protocol2or3ifFL.add(new JumpInsnNode(GOTO, end));
            protocol2or3ifFL.add(noFL);
            protocol2or3ifFL.add(new InsnNode(ICONST_2));
            protocol2or3ifFL.add(end);
            getHandlerProtocol.instructions.insert(iconst2, protocol2or3ifFL);
            getHandlerProtocol.instructions.remove(iconst2);
        }
        return classNode;
    }
}
