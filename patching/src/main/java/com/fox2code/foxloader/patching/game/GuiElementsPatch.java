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
