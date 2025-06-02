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

final class KeyBindingPatch extends GamePatch {
    private static final String KeyBindingAPI$Internal = "com/fox2code/foxloader/client/KeyBindingAPI$Internal";
    private static final String GameSettings = "net/minecraft/client/util/GameSettings";
    private static final String KeyBinding = "net/minecraft/client/util/KeyBinding";

    KeyBindingPatch() {
        super(GameSettings);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (!GameSettings.equals(classNode.name)) return classNode;
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "init");
        AbstractInsnNode returnInsn = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == RETURN) {
                returnInsn = abstractInsnNode;
                break;
            }
        }
        Objects.requireNonNull(returnInsn);
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, GameSettings,
                "keyBindings", "[L" + KeyBinding + ";"));
        insnList.add(new MethodInsnNode(INVOKESTATIC, KeyBindingAPI$Internal,
                "inject", "([L" + KeyBinding + ";)[L" + KeyBinding + ";"));
        insnList.add(new FieldInsnNode(PUTFIELD, GameSettings,
                "keyBindings", "[L" + KeyBinding + ";"));
        methodNode.instructions.insertBefore(returnInsn, insnList);
        return classNode;
    }
}
