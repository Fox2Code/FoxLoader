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

import java.util.Objects;

final class FluidsPatch extends GamePatch {
    private static final String World = "net/minecraft/common/world/World";
    private static final String BlockFluid = "net/minecraft/common/block/children/BlockFluid";
    private static final String InternalFluidsHooks = "com/fox2code/foxloader/internal/InternalFluidsHooks";

    FluidsPatch() {
        super(BlockFluid);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (BlockFluid.equals(classNode.name)) {
            patchBlockFluid(classNode);
        }
        return classNode;
    }

    private static void patchBlockFluid(ClassNode classNode) {
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "flowIntoBlock");
        JumpInsnNode jumpInstance = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == IFEQ) {
                jumpInstance = (JumpInsnNode) abstractInsnNode;
                break;
            }
        }
        Objects.requireNonNull(jumpInstance);
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 1));
        insnList.add(new VarInsnNode(ILOAD, 2));
        insnList.add(new VarInsnNode(ILOAD, 3));
        insnList.add(new VarInsnNode(ILOAD, 4));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ILOAD, 5));
        insnList.add(new MethodInsnNode(INVOKESTATIC, InternalFluidsHooks,
                "onLiquidFlowIntoBlock", "(L" + World + ";IIIL" + BlockFluid + ";I)Z", false));
        insnList.add(new JumpInsnNode(IFNE, jumpInstance.label));
        methodNode.instructions.insert(jumpInstance, insnList);
    }
}
