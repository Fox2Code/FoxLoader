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
package com.fox2code.foxloader.patching;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public final class OpcodesUtils {
    private OpcodesUtils() {

    }

    public static int getStackConsume(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        if (opcode == -1) return 0;

        switch (opcode) {
            case Opcodes.NOP:
            case Opcodes.IINC:
            case Opcodes.LDC:
            case Opcodes.ALOAD:
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.LLOAD:
            case Opcodes.ACONST_NULL:
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
            case Opcodes.RETURN:
            case Opcodes.GETSTATIC:
            case Opcodes.GOTO:
            case Opcodes.NEW:
                return 0;
            case Opcodes.POP:
            case Opcodes.DUP:
            case Opcodes.ASTORE:
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ATHROW:
            case Opcodes.ARETURN:
            case Opcodes.FRETURN:
            case Opcodes.IRETURN:
            case Opcodes.GETFIELD:
            case Opcodes.IFNONNULL:
            case Opcodes.IFNULL:
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.NEWARRAY:
            case Opcodes.ANEWARRAY:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.INSTANCEOF:
            case Opcodes.CHECKCAST:
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                return 1;
            case Opcodes.POP2:
            case Opcodes.SWAP:
            case Opcodes.DUP2:
            case Opcodes.DUP_X1:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.IAND:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.AALOAD:
            case Opcodes.IALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DSTORE:
            case Opcodes.LSTORE:
            case Opcodes.DRETURN:
            case Opcodes.LRETURN:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                return 2;
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X1:
            case Opcodes.DALOAD:
            case Opcodes.LALOAD:
            case Opcodes.IASTORE:
            case Opcodes.FASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                return 3;
            case Opcodes.DUP2_X2:
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
            case Opcodes.LASTORE:
            case Opcodes.DASTORE:
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return 4;
            case Opcodes.PUTSTATIC:
                return Type.getType(((FieldInsnNode) insn).desc).getSize();
            case Opcodes.PUTFIELD:
                return Type.getType(((FieldInsnNode) insn).desc).getSize() + 1;
            case Opcodes.INVOKESTATIC:
                return (Type.getArgumentsAndReturnSizes(((MethodInsnNode) insn).desc) >> 2) - 1;
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEINTERFACE:
                return Type.getArgumentsAndReturnSizes(((MethodInsnNode) insn).desc) >> 2;
            case Opcodes.INVOKEDYNAMIC:
                return (Type.getArgumentsAndReturnSizes(((InvokeDynamicInsnNode) insn).desc) >> 2) - 1;
            case Opcodes.MULTIANEWARRAY:
                return ((MultiANewArrayInsnNode) insn).dims;
            default:
                throw new IllegalArgumentException("Unsupported stack opcode: " + opcode);
        }
    }

    public static int getStackProduce(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        if (opcode == -1) return 0;

        switch (opcode) {
            case Opcodes.NOP:
            case Opcodes.IINC:
            case Opcodes.POP:
            case Opcodes.POP2:
            case Opcodes.ASTORE:
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ATHROW:
            case Opcodes.ARETURN:
            case Opcodes.FRETURN:
            case Opcodes.IRETURN:
            case Opcodes.DRETURN:
            case Opcodes.LRETURN:
            case Opcodes.RETURN:
            case Opcodes.PUTFIELD:
            case Opcodes.PUTSTATIC:
            case Opcodes.IFNONNULL:
            case Opcodes.IFNULL:
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.GOTO:
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                return 0;
            case Opcodes.ALOAD:
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.ACONST_NULL:
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.IAND:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.AALOAD:
            case Opcodes.IALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.FALOAD:
            case Opcodes.NEWARRAY:
            case Opcodes.ANEWARRAY:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.INSTANCEOF:
            case Opcodes.CHECKCAST:
            case Opcodes.NEW:
            case Opcodes.I2F:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.F2I:
            case Opcodes.D2I:
            case Opcodes.D2F:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.LCMP:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.MULTIANEWARRAY:
                return 1;
            case Opcodes.DLOAD:
            case Opcodes.LLOAD:
            case Opcodes.DUP:
            case Opcodes.SWAP:
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
            case Opcodes.DALOAD:
            case Opcodes.LALOAD:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.I2L:
            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.D2L:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
                return 2;
            case Opcodes.DUP_X1:
                return 3;
            case Opcodes.DUP_X2:
            case Opcodes.DUP2:
                return 4;
            case Opcodes.DUP2_X1:
                return 5;
            case Opcodes.DUP2_X2:
                return 6;
            case Opcodes.GETSTATIC:
            case Opcodes.GETFIELD:
                return Type.getType(((FieldInsnNode) insn).desc).getSize();
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEINTERFACE:
                return Type.getArgumentsAndReturnSizes(((MethodInsnNode) insn).desc) & 3;
            case Opcodes.INVOKEDYNAMIC:
                return Type.getArgumentsAndReturnSizes(((InvokeDynamicInsnNode) insn).desc) & 3;
            case Opcodes.LDC:
                Object value = ((LdcInsnNode) insn).cst;
                return (value instanceof Double || value instanceof Long) ? 2 : 1;
            default:
                throw new IllegalArgumentException("Unsupported stack opcode: " + opcode);
        }
    }
}
