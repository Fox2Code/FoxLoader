package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

final class GuiElementsPatch extends GamePatch {
    private static final String GuiSlot = "net/minecraft/client/gui/GuiSlot";
    private static final String GuiButton = "net/minecraft/client/gui/GuiButton";

    GuiElementsPatch() {
        super(new String[]{GuiSlot, GuiButton});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        switch (classNode.name) {
            case GuiSlot: {
                transformGuiSlot(classNode);
                break;
            }
            case GuiButton: {
                transformGuiButton(classNode);
                break;
            }
        }
        return classNode;
    }

    private static void transformGuiSlot(ClassNode classNode) {
        for (String fieldName : new String[]{"amountScrolled", "headerPadding"}) {
            FieldNode fieldNode = TransformerUtils.getField(classNode, fieldName);
            if ((fieldNode.access & (ACC_PRIVATE | ACC_PUBLIC)) == ACC_PRIVATE) {
                fieldNode.access &= ~ACC_PRIVATE;
                fieldNode.access |= ACC_PROTECTED;
            }
        }
        for (String methodName : new String[]{"bindAmountScrolled"}) {
            MethodNode methodNode = TransformerUtils.getMethod(classNode, methodName);
            int origAccess = methodNode.access;
            if ((methodNode.access & (ACC_PRIVATE | ACC_PUBLIC)) != ACC_PUBLIC) {
                methodNode.access &= ~ACC_PRIVATE;
                methodNode.access |= ACC_PROTECTED;
                if ((origAccess & (ACC_PROTECTED | ACC_PUBLIC | ACC_PRIVATE)) == ACC_PRIVATE) {
                    methodNode.access |= ACC_FINAL;
                }
            }
        }
    }

    private static void transformGuiButton(ClassNode classNode) {
        for (String fieldName : new String[]{"xPosition", "yPosition", "width", "height"}) {
            FieldNode fieldNode = TransformerUtils.getField(classNode, fieldName);
            fieldNode.access &= ~(ACC_FINAL | ACC_PRIVATE | ACC_PROTECTED);
            fieldNode.access |= ACC_PUBLIC;
        }
    }
}
