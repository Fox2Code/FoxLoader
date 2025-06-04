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
package com.fox2code.foxloader.patching.test;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Objects;

public class TransformerUtilsTest {
    @Test
    public void testGetConsumingInstruction() {
        InsnList insnList = new InsnList();
        MethodInsnNode first;
        AbstractInsnNode expected;
        insnList.add(first = new MethodInsnNode(Opcodes.INVOKESTATIC,
                "EnumObject", "values", "()[LEnumObject;", false));
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new FieldInsnNode(Opcodes.GETFIELD,
                "SelfObject", "mMethodObject", "LMethodObject;"));
        insnList.add(TransformerUtils.getNumberInsn(18));
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "MethodObject", "convertIntToByte", "(I)B", false));
        insnList.add(expected = new InsnNode(Opcodes.AALOAD));
        insnList.add(new InsnNode(Opcodes.ARETURN));
        AbstractInsnNode actual = TransformerUtils.getConsumingInstruction(insnList,
                Objects.requireNonNull(first.getNext(), "first -> next"));
        Assertions.assertSame(expected, actual, () -> "Expected: " +
                expected.getOpcode() + " (Index:" + insnList.indexOf(expected) + ") but got " +
                actual.getOpcode() + " (Index: " + insnList.indexOf(actual) + ")");
    }
}
