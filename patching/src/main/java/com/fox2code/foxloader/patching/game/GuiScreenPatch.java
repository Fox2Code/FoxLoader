package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

public final class GuiScreenPatch extends GamePatch {
    private static final String Minecraft = "net/minecraft/client/Minecraft";
    private static final String GuiDebug = "net/minecraft/client/gui/GuiDebug";
    private static final String GuiScreen = "net/minecraft/client/gui/GuiScreen";
    private static final String GuiContainer = "net/minecraft/client/gui/GuiContainer";
    private static final String GuiMainMenu = "net/minecraft/client/gui/GuiMainMenu";
    private static final String InternalScreenHooks = "com/fox2code/foxloader/internal/InternalScreenHooks";

    GuiScreenPatch() {
        super(ALL_CLASSES);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case Minecraft: {
                patchMinecraft(classNode);
                break;
            }
            case GuiDebug: {
                patchGuiDebug(classNode);
                break;
            }
            case GuiScreen: {
                patchGuiScreen(classNode);
                break;
            }
            case GuiContainer: {
                patchGuiContainer(classNode);
                break;
            }
            case GuiMainMenu: {
                patchGuiMainMenu(classNode);
                break;
            }
        }
        if (classNode.name.startsWith("net/minecraft/client/gui/") ||
                classNode.superName.startsWith("net/minecraft/client/gui/")) {
            for (String methodName : new String[]{"mouseClicked", "keyTyped", "mouseMovedOrUp", "getSlotAtPosition"}) {
                MethodNode methodNode = TransformerUtils.findMethod(classNode, methodName);
                if (methodNode != null && (methodNode.access & ~ACC_FINAL) == ACC_PROTECTED) {
                    methodNode.access = ACC_PUBLIC;
                }
            }
        }
        return classNode;
    }

    private static void patchMinecraft(ClassNode classNode) {}

    private static void patchGuiDebug(ClassNode classNode) {
        MethodNode drawScreen = TransformerUtils.getMethod(classNode, "drawScreen");
        for (AbstractInsnNode abstractInsnNode : drawScreen.instructions) {
            if (abstractInsnNode instanceof LdcInsnNode) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                if (ldcInsnNode.cst instanceof String &&
                        ((String) ldcInsnNode.cst).startsWith("ReIndev " + BuildConfig.REINDEV_VERSION)) {
                    String constant = ((String) ldcInsnNode.cst);
                    if (constant.endsWith(")")) {
                        // This branch happens for betas and such
                        ldcInsnNode.cst = constant.substring(0, constant.length() - 1) +
                                " / FoxLoader " + BuildConfig.FOXLOADER_VERSION + ")";
                    } else {
                        ldcInsnNode.cst = constant + " (FoxLoader " + BuildConfig.FOXLOADER_VERSION + ")";
                    }
                }
            }
        }
    }

    private static void patchGuiScreen(ClassNode classNode) {
        MethodNode setWorldAndResolution = TransformerUtils.getMethod(classNode, "setWorldAndResolution");
        setWorldAndResolution.access |= ACC_FINAL;
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, GuiScreen, "controlList", "Ljava/util/List;"));
        insnList.add(new MethodInsnNode(INVOKESTATIC, InternalScreenHooks,
                "onGuiScreenInitHook", "(L" + GuiScreen + ";Ljava/util/List;)V"));
        TransformerUtils.insertToEndOfCode(setWorldAndResolution, insnList);
    }

    private void patchGuiContainer(ClassNode classNode) {
        TransformerUtils.makeGetterForFields(classNode, "cursorSlot", "xSize", "ySize");
        FieldNode fieldNode = TransformerUtils.getField(classNode, "xSize");
        fieldNode.access &= ~ACC_FINAL;
    }

    private static void patchGuiMainMenu(ClassNode classNode) {
        final String reIndevVersionPattern = "ReIndev " + BuildConfig.REINDEV_VERSION;
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "drawScreen");
        LdcInsnNode begin = null;
        AbstractInsnNode end = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == LDC) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                if (ldcInsnNode.cst instanceof String &&
                        ((String) ldcInsnNode.cst).startsWith(reIndevVersionPattern)) {
                    begin = ldcInsnNode;
                }
            } else if (begin != null && abstractInsnNode.getOpcode() == INVOKEVIRTUAL) {
                end = abstractInsnNode;
                break;
            }
        }
        InsnList copiedInsnList = TransformerUtils.copyCodeUntil(begin, INVOKEVIRTUAL);
        for (AbstractInsnNode abstractInsnNode : copiedInsnList) {
            if (abstractInsnNode.getOpcode() == LDC) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode;
                if (ldcInsnNode.cst instanceof String &&
                        ((String) ldcInsnNode.cst).startsWith(reIndevVersionPattern)) {
                    ldcInsnNode.cst = "FoxLoader " + BuildConfig.FOXLOADER_VERSION;
                }
            } else if (abstractInsnNode.getOpcode() == BIPUSH) {
                if(((IntInsnNode) abstractInsnNode).operand == 10) {
                    ((IntInsnNode) abstractInsnNode).operand = 22;
                }
            }
        }
        methodNode.instructions.insert(end, copiedInsnList);
    }
}
