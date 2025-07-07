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

final class MovementPatch extends GamePatch {
    private static final String Entity = "net/minecraft/common/entity/Entity";
    private static final String EntityLiving = "net/minecraft/common/entity/EntityLiving";
    private static final String EntityPlayer = "net/minecraft/common/entity/player/EntityPlayer";
    private static final String InternalMovementHooks = "com/fox2code/foxloader/internal/InternalMovementHooks";

    MovementPatch() {
        super(EntityPlayer);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (EntityPlayer.equals(classNode.name)) {
            patchEntityPlayer(classNode);
        }
        return classNode;
    }

    private static void patchEntityPlayer(ClassNode classNode) {
        classNode.fields.add(new FieldNode(ACC_PUBLIC, "flWasSneaking", "Z", null, null));
        classNode.fields.add(new FieldNode(ACC_PUBLIC, "flWasJumping", "Z", null, null));
        MethodNode onLivingUpdate = TransformerUtils.getMethod(classNode, "onLivingUpdate");
        InsnList append = new InsnList();
        append.add(new VarInsnNode(ALOAD, 0));
        append.add(new MethodInsnNode(INVOKESTATIC, InternalMovementHooks,
                "onPlayerLivingUpdated", "(L" + EntityPlayer + ";)V"));
        TransformerUtils.insertToEndOfCode(onLivingUpdate, append);
    }
}
