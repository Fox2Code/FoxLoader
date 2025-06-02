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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

final class PlayerInteractionsPatch extends GamePatch {
    private static final String World = "net/minecraft/common/world/World";
    private static final String Vec3D = "net/minecraft/common/util/math/Vec3D";
    private static final String ItemStack = "net/minecraft/common/item/ItemStack";
    private static final String Entity = "net/minecraft/common/entity/Entity";
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String EntityPlayerMP = "net/minecraft/server/entity/player/EntityPlayerMP";
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String EntityPlayerSP = "net/minecraft/client/player/EntityPlayerSP";
    private static final String PlayerControllerClient = "net/minecraft/client/player/PlayerController";
    private static final String PlayerControllerTest = "net/minecraft/client/player/PlayerControllerTest";
    private static final String PlayerControllerMP = "net/minecraft/client/player/PlayerControllerMP";
    private static final String PlayerControllerSP = "net/minecraft/client/player/PlayerControllerSP";
    private static final String PlayerControllerServer = "net/minecraft/server/entity/player/PlayerController";
    private static final String InternalInteractionHooks = "com/fox2code/foxloader/internal/InternalInteractionHooks";
    private static final String NetworkBlockUpdateHelper = "com/fox2code/foxloader/server/NetworkBlockUpdateHelper";

    PlayerInteractionsPatch() {
        super(new String[]{EntityPlayer, PlayerControllerClient, PlayerControllerTest,
                PlayerControllerMP, PlayerControllerSP, PlayerControllerServer});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (EntityPlayer.equals(classNode.name)) {
            patchEntityPlayer(classNode);
        } else if (PlayerControllerMP.equals(classNode.name) ||
                PlayerControllerSP.equals(classNode.name) ||
                PlayerControllerTest.equals(classNode.name)) {
            patchClientPlayerControllers(classNode);
        } else if (PlayerControllerServer.equals(classNode.name)) {
            patchServerPlayerController(classNode);
        }
        return classNode;
    }

    private static void patchEntityPlayer(ClassNode classNode) {
        hookMethodForInteraction(classNode, "useCurrentItemOnEntity",
                "(L" + Entity + ";)V", "sendPlayerUseItemOnEntityEvent", false);
        hookMethodForInteraction(classNode, "attackTargetEntityWithCurrentItem",
                "(L" + Entity + ";)V", "sendPlayerAttackEntityEvent", false);
    }

    private static void patchClientPlayerControllers(ClassNode classNode) {
        hookMethodForInteraction(classNode, "clickBlock", "(IIII)V", "sendPlayerStartBreakBlockEvent", false);
        hookMethodForInteraction(classNode, "sendBlockRemoved", "(IIII)Z", "sendPlayerBreakBlockEvent", true);
        hookMethodForInteraction(classNode, "sendPlaceBlock",
                "(L" + EntityPlayer + ";L" + World + ";L" + ItemStack + ";IIIIL" + Vec3D + ";)Z",
                "sendPlayerUseItemOnBlockEvent", false);
        hookMethodForInteraction(classNode, "sendUseItem",
                "(L" + EntityPlayer + ";L" + World + ";L" + ItemStack + ";)Z",
                "sendPlayerUseItemOnAirEvent", false);
    }

    private static void patchServerPlayerController(ClassNode classNode) {
        hookMethodForInteraction(classNode, "blockClicked", "(IIII)V", "sendPlayerStartBreakBlockEvent", false);
        hookMethodForInteraction(classNode, "harvestBlock", "(III)Z", "sendPlayerBreakBlockEvent", false);
        hookMethodForInteraction(classNode, "activeBlockOrUseItem",
                "(L" + EntityPlayer + ";L" + World + ";L" + ItemStack + ";IIIIFFF)Z",
                "sendPlayerUseItemOnBlockEvent", false);
        hookMethodForInteraction(classNode, "itemUsed",
                "(L" + EntityPlayer + ";L" + World + ";L" + ItemStack + ";)Z",
                "sendPlayerUseItemOnAirEvent", false);
    }

    private static void hookMethodForInteraction(
            ClassNode classNode, String methodToHook, String methodDesc, String helperMethod, boolean rmLastArg) {
        boolean startWithPlayer = methodDesc.startsWith("(L" + EntityPlayer + ";");
        boolean server = PlayerControllerServer.equals(classNode.name);
        String helperMethodDesc = startWithPlayer ? methodDesc :
                methodDesc.replace("(", "(L" + EntityPlayer + ";");
        helperMethodDesc = helperMethodDesc.substring(0,
                helperMethodDesc.indexOf(')') + (rmLastArg ? -1 : 0)) + ")Z";
        Type methodDescType = Type.getMethodType(methodDesc);
        Type[] arguments = methodDescType.getArgumentTypes();
        Type returnType = methodDescType.getReturnType();
        MethodNode methodNode = TransformerUtils.findMethod(classNode, methodToHook, methodDesc);
        if (methodNode == null) {
            if (PlayerControllerClient.equals(classNode.name) || server) {
                // We cannot call super here, so the code probably need some fixing.
                throw new RuntimeException("Could not find " + classNode.name + "." + methodToHook + methodDesc);
            }
            methodNode = new MethodNode(ACC_PUBLIC, methodToHook, methodDesc, null, null);
            InsnList insnList = methodNode.instructions;
            if (!startWithPlayer) getPlayer(classNode, insnList, false);
            getAllArguments(insnList, arguments, rmLastArg);
            insnList.add(new MethodInsnNode(INVOKESTATIC, InternalInteractionHooks, helperMethod, helperMethodDesc));
            LabelNode labelNode = new LabelNode();
            insnList.add(new JumpInsnNode(IFNE, labelNode));
            insnList.add(new VarInsnNode(ALOAD, 0));
            getAllArguments(insnList, arguments, false);
            insnList.add(new MethodInsnNode(INVOKESPECIAL, classNode.superName, methodToHook, methodDesc));
            if (returnType == Type.VOID_TYPE) {
                insnList.add(labelNode);
                insnList.add(new InsnNode(RETURN));
            } else {
                insnList.add(new InsnNode(IRETURN));
                insnList.add(labelNode);
                insnList.add(new InsnNode(ICONST_0));
                insnList.add(new InsnNode(IRETURN));
            }
            classNode.methods.add(methodNode);
        } else {
            InsnList prelude = new InsnList();
            if (!startWithPlayer) getPlayer(classNode, prelude, server);
            getAllArguments(prelude, arguments, rmLastArg);
            prelude.add(new MethodInsnNode(INVOKESTATIC, InternalInteractionHooks, helperMethod, helperMethodDesc));
            LabelNode labelNode = new LabelNode();
            prelude.add(new JumpInsnNode(IFEQ, labelNode));
            sendOutNetworkUpdate(classNode, prelude, arguments, server);
            if (returnType == Type.VOID_TYPE) {
                prelude.add(new InsnNode(RETURN));
            } else {
                prelude.add(new InsnNode(ICONST_0));
                prelude.add(new InsnNode(IRETURN));
            }
            prelude.add(labelNode);
            TransformerUtils.insertToBeginningOfCode(methodNode, prelude);
        }
    }

    private static void getAllArguments(InsnList insnList, Type[] arguments, boolean rmLastArg) {
        int index = 1;
        int sizeM1 = rmLastArg ? arguments.length - 1 : -1;
        for (int i = 0; i < arguments.length; i++) {
            if (sizeM1 == i) return;
            Type argument = arguments[i];
            insnList.add(new VarInsnNode(argument.getOpcode(ILOAD), index));
            index += argument.getSize();
        }
    }

    private static void getPlayer(ClassNode classNode, InsnList insnList, boolean server) {
        insnList.add(new VarInsnNode(ALOAD, 0));
        if (EntityPlayer.equals(classNode.name)) {
            return;
        }
        if (server) {
            insnList.add(new FieldInsnNode(GETFIELD, classNode.name, "player", "L" + EntityPlayer + ";"));
        } else {
            insnList.add(new FieldInsnNode(GETFIELD, classNode.name, "mc", "L" + Minecraft + ";"));
            insnList.add(new FieldInsnNode(GETFIELD, Minecraft, "thePlayer", "L" + EntityPlayerSP + ";"));
        }
    }

    private static void sendOutNetworkUpdate(ClassNode classNode, InsnList insnList, Type[] arguments, boolean server) {
        if (!server) return;
        int argIntIndex = -1;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == Type.INT_TYPE) {
                argIntIndex = i;
                break;
            }
            if (arguments[i].getSize() != 1) {
                break;
            }
        }
        if (argIntIndex == -1) return;
        getPlayer(classNode, insnList, true);
        insnList.add(new TypeInsnNode(CHECKCAST, EntityPlayerMP));
        insnList.add(new VarInsnNode(ILOAD, 1 + argIntIndex));
        insnList.add(new VarInsnNode(ILOAD, 2 + argIntIndex));
        insnList.add(new VarInsnNode(ILOAD, 3 + argIntIndex));
        if (arguments.length >= (4 + argIntIndex) && arguments[3 + argIntIndex] == Type.INT_TYPE) {
            insnList.add(new VarInsnNode(ILOAD, 4 + argIntIndex));
        } else {
            insnList.add(new InsnNode(ICONST_M1));
        }
        insnList.add(new FieldInsnNode(INVOKESTATIC, NetworkBlockUpdateHelper,
                "sendBlockUpdate", "(L" + EntityPlayerMP + ";IIII)V"));
    }
}
