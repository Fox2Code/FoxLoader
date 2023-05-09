package com.fox2code.foxloader.loader.transformer;

import com.fox2code.foxloader.launcher.ClassTransformer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class TransformerUtils {
    private static final String[] STRING_ARRAY = new String[0];

    public static MethodNode copyMethodNode(MethodNode methodNode) {
        MethodNode methodNodeCopy = new MethodNode(ClassTransformer.ASM_BUILD, methodNode.access,
                methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(STRING_ARRAY));
        Map<LabelNode, LabelNode> map = new IdentityHashMap<LabelNode, LabelNode>() {
            @Override
            public LabelNode get(Object key) {
                LabelNode labelNode = super.get(key);
                return labelNode == null ? (LabelNode) key : labelNode;
            }
        };
        InsnList insnList = methodNode.instructions;
        for (AbstractInsnNode abstractInsnNode : insnList) {
            if (abstractInsnNode instanceof LabelNode) {
                map.put((LabelNode) abstractInsnNode, new LabelNode());
            }
        }
        if (methodNode.localVariables != null && !methodNode.localVariables.isEmpty()) {
            if (methodNodeCopy.localVariables == null)
                methodNodeCopy.localVariables = new ArrayList<>();
            for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                methodNodeCopy.localVariables.add(new LocalVariableNode(
                        localVariableNode.name, localVariableNode.desc, localVariableNode.desc,
                        map.get(localVariableNode.start), map.get(localVariableNode.end), localVariableNode.index));
            }
        }
        InsnList copy = methodNodeCopy.instructions;
        for (AbstractInsnNode abstractInsnNode : insnList) {
            copy.add(abstractInsnNode.clone(map));
        }
        methodNodeCopy.maxLocals = methodNode.maxLocals;
        methodNodeCopy.maxStack = methodNode.maxStack;
        if (methodNode.tryCatchBlocks != null &&
                !methodNode.tryCatchBlocks.isEmpty()) {
            if (methodNodeCopy.tryCatchBlocks == null)
                methodNodeCopy.tryCatchBlocks = new ArrayList<>();
            for (TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                methodNodeCopy.tryCatchBlocks.add(new TryCatchBlockNode(map.get(tryCatchBlockNode.start),
                        map.get(tryCatchBlockNode.end), map.get(tryCatchBlockNode.handler), tryCatchBlockNode.type));
            }
        }
        return methodNodeCopy;
    }

    @NotNull
    public static InsnList copyCodeUntil(final AbstractInsnNode start, int endOpCode) {
        AbstractInsnNode abstractInsnNode = start;
        Map<LabelNode, LabelNode> map = new IdentityHashMap<LabelNode, LabelNode>() {
            @Override
            public LabelNode get(Object key) {
                LabelNode labelNode = super.get(key);
                return labelNode == null ? (LabelNode) key : labelNode;
            }
        };
        while (abstractInsnNode != null &&
                abstractInsnNode.getOpcode() != endOpCode) {
            if (abstractInsnNode instanceof LabelNode) {
                map.put((LabelNode) abstractInsnNode, new LabelNode());
            }
            abstractInsnNode = abstractInsnNode.getNext();
        }
        if (abstractInsnNode == null) {
            throw new IllegalArgumentException("Opcodes " + endOpCode + " isn't present after the given instruction");
        }
        InsnList copy = new InsnList();
        abstractInsnNode = start;
        while (abstractInsnNode.getOpcode() != endOpCode) {
            copy.add(abstractInsnNode.clone(map));
            abstractInsnNode = abstractInsnNode.getNext();
        }
        copy.add(abstractInsnNode.clone(map));
        return copy;
    }

    @NotNull
    public static MethodNode getMethod(ClassNode classNode, String methodName) {
        return findMethod0(classNode, methodName, null, true);
    }

    @NotNull
    public static MethodNode getMethod(ClassNode classNode, String methodName, String methodDesc) {
        return findMethod0(classNode, methodName, methodDesc, true);
    }

    @Nullable
    public static MethodNode findMethod(ClassNode classNode, String methodName) {
        return findMethod0(classNode, methodName, null, false);
    }

    @Nullable
    public static MethodNode findMethod(ClassNode classNode, String methodName, String methodDesc) {
        return findMethod0(classNode, methodName, methodDesc, false);
    }

    @Contract("_, _, _, true -> !null")
    private static MethodNode findMethod0(ClassNode classNode, String methodName, String methodDesc, boolean require) {
        for (MethodNode methodNode:classNode.methods) {
            if (methodNode.name.equals(methodName)
                    && (methodDesc == null || methodNode.desc.equals(methodDesc))) {
                return methodNode;
            }
        }
        if (require) {
            throw new NoSuchElementException(classNode.name + "." +
                    methodName + (methodDesc == null ? "()" : methodDesc));
        } else {
            return null;
        }
    }

    public static FieldNode getField(ClassNode classNode,String fieldName) {
        for (FieldNode fieldNode:classNode.fields) {
            if (fieldNode.name.equals(fieldName)) {
                return fieldNode;
            }
        }
        throw new NoSuchElementException(classNode.name + "." + fieldName);
    }

    public static AbstractInsnNode getNumberInsn(int number) {
        if (number >= -1 && number <= 5)
            return new InsnNode(number + 3);
        else if (number >= -128 && number <= 127)
            return new IntInsnNode(Opcodes.BIPUSH, number);
        else if (number >= -32768 && number <= 32767)
            return new IntInsnNode(Opcodes.SIPUSH, number);
        else
            return new LdcInsnNode(number);
    }

    public static String printInsnList(InsnList insnList) {
        final StringBuilder stringBuilder = new StringBuilder();
        printInsnList(insnList, stringBuilder);
        return stringBuilder.toString();
    }

    public static void printInsnList(final InsnList insnList,final StringBuilder stringBuilder) {
        Textifier textifier = new Textifier();
        MethodNode methodNode = new MethodNode(0, "insns", "()V", null, null);
        methodNode.instructions = insnList;
        methodNode.accept(new TraceMethodVisitor(textifier));
        textifier.print(new PrintWriter(new Writer() {
            @Override
            public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                stringBuilder.append(cbuf, off, len);
            }

            @Override public void flush() {}
            @Override public void close() {}
        }));
    }
}
