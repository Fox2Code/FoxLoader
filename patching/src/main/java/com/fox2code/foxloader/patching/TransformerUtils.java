package com.fox2code.foxloader.patching;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

public final class TransformerUtils {
    public static final int ASM_BUILD = Opcodes.ASM9;
    private static final String[] STRING_ARRAY = new String[0];

    public static MethodNode copyMethodNode(MethodNode methodNode) {
        MethodNode methodNodeCopy = new MethodNode(ASM_BUILD, methodNode.access,
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

    public static InsnList copyInsnList(InsnList insnList) {
        Map<LabelNode, LabelNode> map = new IdentityHashMap<LabelNode, LabelNode>() {
            @Override
            public LabelNode get(Object key) {
                LabelNode labelNode = super.get(key);
                return labelNode == null ? (LabelNode) key : labelNode;
            }
        };
        for (AbstractInsnNode abstractInsnNode : insnList) {
            if (abstractInsnNode instanceof LabelNode) {
                map.put((LabelNode) abstractInsnNode, new LabelNode());
            }
        }
        InsnList copy = new InsnList();
        for (AbstractInsnNode abstractInsnNode : insnList) {
            copy.add(abstractInsnNode.clone(map));
        }
        return copy;
    }

    @NotNull
    public static InsnList copyCodeUntil(final AbstractInsnNode start, int endOpCode) {
        AbstractInsnNode abstractInsnNode = start;
        IdentityHashMap<LabelNode, LabelNode> map = new IdentityHashMap<LabelNode, LabelNode>() {
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
        MethodNode bridgeMethodNode = null;
        for (MethodNode methodNode:classNode.methods) {
            if (methodNode.name.equals(methodName)
                    && (methodDesc == null || methodNode.desc.equals(methodDesc))) {
                if ((methodNode.access & Opcodes.ACC_BRIDGE) != 0) {
                    bridgeMethodNode = methodNode;
                } else return methodNode;
            }
        }
        if (bridgeMethodNode != null) {
            return bridgeMethodNode;
        }
        if (require) {
            throw new NoSuchElementException(classNode.name + "." +
                    methodName + (methodDesc == null ? "()" : methodDesc));
        } else {
            return null;
        }
    }

    public static FieldNode getField(ClassNode classNode,String fieldName) {
        return findField0(classNode, fieldName, null, true);
    }

    public static FieldNode getField(ClassNode classNode, String fieldName, String fieldDesc) {
        return findField0(classNode, fieldName, fieldDesc, true);
    }

    public static FieldNode findField(ClassNode classNode,String fieldName) {
        return findField0(classNode, fieldName, null, false);
    }

    public static FieldNode findField(ClassNode classNode, String fieldName, String fieldDesc) {
        return findField0(classNode, fieldName, fieldDesc, false);
    }

    private static FieldNode findField0(ClassNode classNode, String fieldName, String fieldDesc, boolean require) {
        for (FieldNode fieldNode:classNode.fields) {
            if (fieldNode.name.equals(fieldName) &&
                    (fieldDesc == null || fieldNode.desc.equals(fieldDesc))) {
                return fieldNode;
            }
        }
        if (require) {
            throw new NoSuchElementException(classNode.name + "." +
                    fieldName + (fieldDesc == null ? "" : " " + fieldDesc));
        } else {
            return null;
        }
    }

    public static FieldNode getFieldDesc(ClassNode classNode, String fieldDesc) {
        return findFieldDesc0(classNode, fieldDesc, true);
    }

    public static FieldNode findFieldDesc(ClassNode classNode, String fieldDesc) {
        return findFieldDesc0(classNode, fieldDesc, false);
    }

    private static FieldNode findFieldDesc0(ClassNode classNode, String fieldDesc, boolean required) {
        for (FieldNode fieldNode:classNode.fields) {
            if (fieldNode.desc.equals(fieldDesc)) {
                return fieldNode;
            }
        }
        if (required) {
            throw new NoSuchElementException(classNode.name + ".* " + fieldDesc);
        }
        return null;
    }

    public static AbstractInsnNode getAbstractInsnNode(MethodNode methodNode, int opcode) {
        return getAbstractInsnNode(methodNode.instructions, opcode);
    }

    public static AbstractInsnNode getAbstractInsnNode(InsnList insnList, int opcode) {
        for (AbstractInsnNode abstractInsnNode : insnList) {
            if (abstractInsnNode.getOpcode() == opcode) {
                return abstractInsnNode;
            }
        }
        throw new NoSuchElementException("No such opcode: " + opcode);
    }

    public static AbstractInsnNode getBooleanInsn(boolean bool) {
        return new InsnNode(bool ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
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

    public static void addMethodBefore(ClassNode classNode, String methodName, MethodNode methodNode) {
        addMethod0(classNode, methodName, methodNode, true);
    }

    public static void addMethodAfter(ClassNode classNode, String methodName, MethodNode methodNode) {
        addMethod0(classNode, methodName, methodNode, false);
    }

    private static void addMethod0(ClassNode classNode, String methodName, MethodNode methodNode, boolean before) {
        MethodNode targetMethodNode = findMethod0(classNode, methodName, null, true);
        ListIterator<MethodNode> methodNodeListIterator = classNode.methods.listIterator();
        while (methodNodeListIterator.hasNext()) {
            if (methodNodeListIterator.next() == targetMethodNode) {
                if (before) methodNodeListIterator.previous();
                methodNodeListIterator.add(methodNode);
                break;
            }
        }
    }


    public static void addFieldBefore(ClassNode classNode, String fieldName, FieldNode fieldNode) {
        addField0(classNode, fieldName, fieldNode, true);
    }

    public static void addFieldAfter(ClassNode classNode, String fieldName, FieldNode fieldNode) {
        addField0(classNode, fieldName, fieldNode, false);
    }

    private static void addField0(ClassNode classNode, String fieldName, FieldNode fieldNode, boolean before) {
        FieldNode targetFieldNode = getField(classNode, fieldName, null);
        ListIterator<FieldNode> fieldNodeListIterator = classNode.fields.listIterator();
        while (fieldNodeListIterator.hasNext()) {
            if (fieldNodeListIterator.next() == targetFieldNode) {
                if (before) fieldNodeListIterator.previous();
                fieldNodeListIterator.add(fieldNode);
                break;
            }
        }
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
            public void write(@NotNull String str, int off, int len) {
                stringBuilder.append(str, off, len);
            }

            @Override
            public void write(char @NotNull [] cbuf, int off, int len) {
                stringBuilder.append(cbuf, off, len);
            }

            @Override public void flush() {}
            @Override public void close() {}
        }));
    }

    static void checkClassNodeValidity(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        checkBytecodeValidity(classWriter.toByteArray(), false);
    }

    public static void checkBytecodeValidity(byte[] bytes) {
        checkBytecodeValidity(bytes, false);
    }

    /**
     * @param bytes raw class
     * @param checkAsIs if frames and max values should be checked.
     */
    public static void checkBytecodeValidity(final byte[] bytes, boolean checkAsIs) {
        final String[] fullMethodNameRegister = new String[]{null, null, null};
        try {
            new ClassReader(bytes).accept(new ClassVisitor(ASM_BUILD, new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    return "java/lang/Object";
                }
            }) {
                String className;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if (name == null) throw new RuntimeException("Name is null");
                    if (superName == null) throw new RuntimeException("Super name is null");
                    super.visit(version, access, name, signature, superName, interfaces);
                    this.className = name;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    final String fullMethodName = this.className + "." + name + descriptor;
                    final String methodName = name;
                    final String methodDescriptor = descriptor;
                    fullMethodNameRegister[0] = fullMethodName;
                    fullMethodNameRegister[1] = methodName;
                    fullMethodNameRegister[2] = methodDescriptor;
                    try {
                        return new AdviceAdapter(ASM_BUILD,
                                super.visitMethod(access, name, descriptor, signature, exceptions),
                                access, name, descriptor) {
                            @Override
                            public void visitMaxs(int maxStack, int maxLocals) {
                                try {
                                    super.visitMaxs(maxStack, maxLocals);
                                } catch (RuntimeException e) {
                                    throw new RuntimeException("For method " + fullMethodName +
                                            dumpMethod(bytes, methodName, methodDescriptor), e);
                                }
                                if (checkAsIs) {
                                    int argSize = Type.getType(descriptor).getArgumentsAndReturnSizes() >> 2;
                                    if ((access & ACC_STATIC) != 0) argSize--;
                                    if (maxLocals < argSize) {
                                        throw new RuntimeException("For method " + fullMethodName +
                                                dumpMethod(bytes, methodName, methodDescriptor),
                                                new RuntimeException("MaxLocals too small got " +
                                                        maxLocals + " but need " + argSize));
                                    }
                                }
                            }

                            @Override
                            public void visitEnd() {
                                try {
                                    super.visitEnd();
                                } catch (RuntimeException e) {
                                    throw new RuntimeException("For method " + fullMethodName +
                                            dumpMethod(bytes, methodName, methodDescriptor), e);
                                }
                            }

                            @Override
                            public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
                                try {
                                    super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
                                } catch (RuntimeException e) {
                                    throw new RuntimeException("For method " + fullMethodName +
                                            dumpMethod(bytes, methodName, methodDescriptor), e);
                                }
                                if (Type.getType(descriptor).getSort() != Type.METHOD) {
                                    final String fullDescriptor = owner + "." + name + " " + descriptor;
                                    throw new RuntimeException("For method " + fullMethodName +
                                            dumpMethod(bytes, methodName, methodDescriptor),
                                            new RuntimeException("Invalid method call " + fullDescriptor));
                                }
                            }
                        };
                    } catch (RuntimeException e) {
                        throw new RuntimeException("For method " + fullMethodName +
                                dumpMethod(bytes, methodName, methodDescriptor), e);
                    }
                }
            }, ClassReader.SKIP_FRAMES);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("For method " + fullMethodNameRegister[0] +
                    dumpMethod(bytes, fullMethodNameRegister[1], fullMethodNameRegister[2]), e);
        }
    }

    private static String dumpMethod(byte[] bytes, String name, String desc) {
        ClassNode classNode = new ClassNode();
        boolean cantReadClassFile = false;
        try {
            new ClassReader(bytes).accept(classNode, 0);
        } catch (Throwable t) {
            cantReadClassFile = true;
        }
        MethodNode methodNode = TransformerUtils.findMethod(classNode, name, desc);
        return methodNode == null ? (cantReadClassFile ?
                (classNode.name == null ? "\nCan't read class file" : "\nCan't fully read class file") :
                "\nMissing method body") : "\n" + TransformerUtils.printInsnList(methodNode.instructions) +
                        (cantReadClassFile ? "\nFailed to fully read class file" : "");
    }

    public static AbstractInsnNode nextCodeInsn(AbstractInsnNode abstractInsnNode, int count) {
        while (count-->0) {
            abstractInsnNode = nextCodeInsn(abstractInsnNode);
        }
        return abstractInsnNode;
    }

    public static AbstractInsnNode nextCodeInsn(AbstractInsnNode abstractInsnNode) {
        do {
            abstractInsnNode = abstractInsnNode.getNext();
        } while (abstractInsnNode != null && abstractInsnNode.getOpcode() == -1);
        return abstractInsnNode;
    }

    public static AbstractInsnNode previousCodeInsn(AbstractInsnNode abstractInsnNode, int count) {
        while (count-->0) {
            abstractInsnNode = previousCodeInsn(abstractInsnNode);
        }
        return abstractInsnNode;
    }

    public static AbstractInsnNode previousCodeInsn(AbstractInsnNode abstractInsnNode) {
        do {
            abstractInsnNode = abstractInsnNode.getPrevious();
        } while (abstractInsnNode != null && abstractInsnNode.getOpcode() == -1);
        return abstractInsnNode;
    }

    public static AbstractInsnNode nextNonCodeInsn(AbstractInsnNode abstractInsnNode) {
        do {
            abstractInsnNode = abstractInsnNode.getNext();
        } while (abstractInsnNode != null && abstractInsnNode.getOpcode() != -1);
        return abstractInsnNode;
    }

    public static AbstractInsnNode previousNonCodeInsn(AbstractInsnNode abstractInsnNode) {
        do {
            abstractInsnNode = abstractInsnNode.getPrevious();
        } while (abstractInsnNode != null && abstractInsnNode.getOpcode() != -1);
        return abstractInsnNode;
    }

    public static void insertToBeginningOfCode(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
        InsnList insnList = new InsnList();
        insnList.add(abstractInsnNode);
        insertToBeginningOfCode(methodNode, insnList);
    }

    public static void insertToBeginningOfCode(MethodNode methodNode, InsnList insnList) {
        AbstractInsnNode first = methodNode.instructions.getFirst();
        if (first.getOpcode() == -1) {
            first = nextCodeInsn(first);
        }
        methodNode.instructions.insertBefore(first, insnList);
    }

    public static void insertAfterConstructor(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
        InsnList insnList = new InsnList();
        insnList.add(abstractInsnNode);
        insertAfterConstructor(methodNode, insnList);
    }

    public static void insertAfterConstructor(MethodNode methodNode, InsnList insnList) {
        AbstractInsnNode first = methodNode.instructions.getFirst();
        while (first.getOpcode() != Opcodes.INVOKESPECIAL ||
                !((MethodInsnNode) first).name.equals("<init>")) {
            first = nextCodeInsn(first);
        }
        methodNode.instructions.insert(first, insnList);
    }

    public static void insertToEndOfCode(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
        InsnList insnList = new InsnList();
        insnList.add(abstractInsnNode);
        insertToEndOfCode(methodNode, insnList);
    }

    public static void insertToEndOfCode(MethodNode methodNode, InsnList insnList) {
        AbstractInsnNode last = methodNode.instructions.getLast();
        if (last.getOpcode() == -1) {
            last = previousCodeInsn(last);
        }
        int lastOpcode = last.getOpcode();
        AbstractInsnNode lastList = insnList.getLast();
        if (lastList.getOpcode() == -1) {
            lastList = previousCodeInsn(lastList);
        }
        int lastListOpcode = lastList.getOpcode();
        boolean sameEnd = lastOpcode == lastListOpcode;
        if (lastOpcode != Opcodes.RETURN && !sameEnd) {
            boolean fail = true;
            if (lastOpcode == Opcodes.IRETURN) {
                AbstractInsnNode previous = TransformerUtils.previousCodeInsn(last);
                int op = previous.getOpcode();
                if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
                    last = previous;
                    fail = false;
                }
            }
            if (fail) {
                throw new RuntimeException("End of code isn't really the end of the code: " + lastOpcode);
            }
        }
        if (sameEnd) {
            methodNode.instructions.insert(last, insnList);
        } else {
            methodNode.instructions.insertBefore(last, insnList);
        }
    }

    public static void removeInstructionsInRange(InsnList insnList, AbstractInsnNode start, AbstractInsnNode end) {
        if (start == end) throw new IllegalStateException("End is start: " + insnList.indexOf(start));
        AbstractInsnNode remInsn = start.getNext();
        AbstractInsnNode nextInsn = remInsn.getNext();
        while (remInsn != end) {
            if (nextInsn == null)
                throw new IllegalStateException("End before start: " + insnList.indexOf(start) + " > " + insnList.indexOf(end));
            insnList.remove(remInsn);
            remInsn = nextInsn;
            nextInsn = remInsn.getNext();
        }
    }

    public static void appendDefaultInitCode(InsnList insnList, String type) {
        insnList.add(new TypeInsnNode(Opcodes.NEW, type));
        insnList.add(new InsnNode(Opcodes.DUP));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, type, "<init>", "()V", false));
    }

    public static void newStringBuilder(InsnList insnList) {
        appendDefaultInitCode(insnList, "java/lang/StringBuilder");
    }

    public static void appendStringBuilder(InsnList insnList) {
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
    }

    public static void appendDebugMarker(InsnList insnList) {
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/fox2code/foxloader/loader/ModLoader$Internal",
                "debugMarker", "()V", false));
    }

    public static void makeGetterForFields(ClassNode classNode, String... fields) {
        for (String field : fields) {
            tryCreateGetter(classNode, TransformerUtils.getField(classNode, field));
        }
    }

    public static void tryCreateGetter(ClassNode classNode, FieldNode fieldNode) {
        tryCreateGetter(classNode, fieldNode, ("Z".equals(fieldNode.desc) ? "is" : "get") +
                fieldNode.name.substring(0, 1).toUpperCase(Locale.ROOT) + fieldNode.name.substring(1));
    }

    public static void tryCreateGetter(ClassNode classNode, FieldNode fieldNode, String getterName) {
        final boolean isStatic = (fieldNode.access & Opcodes.ACC_STATIC) != 0;
        final Type type = Type.getType(fieldNode.desc);
        MethodNode methodNode = TransformerUtils.findMethod(classNode, getterName, "()" + fieldNode.desc);
        if (methodNode != null) {
            if (((methodNode.access & Opcodes.ACC_STATIC) == 0) == isStatic) {
                throw new IllegalArgumentException("Static state missmatch: (Expected " +
                        (isStatic ? "true, got false" : "false, got true") + ")");
            }
            methodNode.access = Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0);
        } else {
            methodNode = new MethodNode(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0),
                    getterName, "()" + fieldNode.desc, null, null);
            if (fieldNode.signature != null) {
                methodNode.signature = "()" + fieldNode.signature;
            }
            InsnList insnList = methodNode.instructions;
            if (!isStatic) {
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            insnList.add(new FieldInsnNode(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
                    classNode.name, fieldNode.name, fieldNode.desc));
            insnList.add(new InsnNode(type.getOpcode(Opcodes.IRETURN)));
            classNode.methods.add(methodNode);
        }
    }

    public static void bringSelfCallToEndOfCode(MethodNode methodNode, String methodName) {
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.ALOAD &&
                    ((VarInsnNode) abstractInsnNode).var == 0) {
                AbstractInsnNode next = abstractInsnNode.getNext();
                if (next.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                    if (methodInsnNode.desc.equals("()V") &&
                            methodInsnNode.name.equals(methodName)) {
                        methodNode.instructions.remove(abstractInsnNode);
                        methodNode.instructions.remove(next);
                        InsnList insnList = new InsnList();
                        insnList.add(abstractInsnNode);
                        insnList.add(next);
                        insertToEndOfCode(methodNode, insnList);
                        return;
                    }
                }
            }
        }
        throw new RuntimeException("Did not found self call!");
    }

    public static void makeFieldPublic(ClassNode classNode, String fieldName) {
        FieldNode fieldNode = TransformerUtils.getField(classNode, fieldName);
        fieldNode.access &= ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
        fieldNode.access |= Opcodes.ACC_PUBLIC;
    }

    public static LabelNode getBeginingLabelNode(MethodNode methodNode) {
        AbstractInsnNode abstractInsnNode = methodNode.instructions.getFirst();
        while (abstractInsnNode != null &&
                abstractInsnNode.getOpcode() == -1) {
            if (abstractInsnNode instanceof LabelNode) {
                return (LabelNode) abstractInsnNode;
            }
            abstractInsnNode = abstractInsnNode.getNext();
        }
        LabelNode endingLabelNode = new LabelNode();
        methodNode.instructions.insert(endingLabelNode);
        return endingLabelNode;
    }

    public static LabelNode getEndingLabelNode(MethodNode methodNode) {
        AbstractInsnNode abstractInsnNode = methodNode.instructions.getLast();
        while (abstractInsnNode != null &&
                abstractInsnNode.getOpcode() == -1) {
            if (abstractInsnNode instanceof LabelNode) {
                return (LabelNode) abstractInsnNode;
            }
            abstractInsnNode = abstractInsnNode.getPrevious();
        }
        LabelNode endingLabelNode = new LabelNode();
        methodNode.instructions.add(endingLabelNode);
        return endingLabelNode;
    }

    public static LocalVariableNode injectLocalVariable(
            MethodNode methodNode, String name, String desc, LabelNode start) {
        int minVal = methodNode.maxLocals;
        for (LocalVariableNode localVariableNode : methodNode.localVariables) {
            minVal = Math.max(minVal, localVariableNode.index +
                    Type.getType(localVariableNode.desc).getSize());
        }
        // TODO: Check code too for minVal?
        LabelNode end = TransformerUtils.getEndingLabelNode(methodNode);
        LocalVariableNode localVariableNode = new LocalVariableNode(name, desc, null, start, end, minVal);
        methodNode.localVariables.add(localVariableNode);
        methodNode.maxLocals = Math.max(methodNode.maxLocals,
                minVal + Type.getType(desc).getSize());
        return localVariableNode;
    }

    public static void setParameterName(MethodNode methodNode, int index, String name) {
        if (index == 0 && (methodNode.access & Opcodes.ACC_STATIC) == 0) {
            throw new IllegalArgumentException("Cannot set arg index zero of a non-static method!");
        }
        setParameterNameImpl(methodNode, index, name);
    }

    public static void setThisParameterName(ClassNode classNode, MethodNode methodNode) {
        if ((methodNode.access & Opcodes.ACC_STATIC) != 0) {
            throw new IllegalArgumentException("Expected a non-static method!");
        }
        setParameterNameImpl(methodNode, 0, classNode.name);
    }

    private static void setParameterNameImpl(MethodNode methodNode, int index, String name) {
        boolean thisField = index == 0 && ((methodNode.access & Opcodes.ACC_STATIC) == 0);
        for (LocalVariableNode localVariableNode : methodNode.localVariables) {
            if (localVariableNode.index == index) {
                localVariableNode.name = thisField ? "this" : name;
                return;
            }
        }
        Type[] args = Type.getArgumentTypes(methodNode.desc);
        int left = index - ((methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0);
        Type type = null;
        if (thisField) {
            type = Type.getObjectType(name);
            name = "this";
        } else {
            for (Type arg : args) {
                if (left == 0) {
                    type = arg;
                    break;
                }
                left -= arg.getSize();
            }
        }
        Objects.requireNonNull(type, "type");
        LabelNode start = getBeginingLabelNode(methodNode);
        LabelNode end = getBeginingLabelNode(methodNode);
        methodNode.localVariables.add(new LocalVariableNode(
                name, type.getDescriptor(), null, start, end, index));
    }

    public static LabelNode getLabelNodeAfter(InsnList insnList, AbstractInsnNode abstractInsnNode) {
        Objects.requireNonNull(insnList, "insnList");
        Objects.requireNonNull(abstractInsnNode, "abstractInsnNode");
        if (abstractInsnNode instanceof LabelNode) {
            return (LabelNode) abstractInsnNode;
        }
        if (abstractInsnNode.getOpcode() == -1) {
            abstractInsnNode = previousCodeInsn(abstractInsnNode);
            if (abstractInsnNode == null) {
                abstractInsnNode = insnList.getFirst();
                if (abstractInsnNode instanceof LabelNode) {
                    return (LabelNode) abstractInsnNode;
                }
            }
        }
        AbstractInsnNode search = abstractInsnNode;
        do {
            search = search.getNext();
            if (search instanceof LabelNode) {
                return (LabelNode) search;
            }
        } while (search != null && search.getOpcode() == -1);
        LabelNode newLabelNode = new LabelNode();
        insnList.insert(abstractInsnNode, newLabelNode);
        return newLabelNode;
    }

    public static void makeIntMethodSymlinkFromFloat(ClassNode classNode, String name, String desc) {
        makeIntMethodSymlinkFromFloat(classNode, getMethod(classNode, name, desc), false);
    }

    public static void makeIntMethodSymlinkFromFloat(
            ClassNode classNode, String name, String desc, boolean returnVoid) {
        makeIntMethodSymlinkFromFloat(classNode, getMethod(classNode, name, desc), returnVoid);
    }

    public static void makeIntMethodSymlinkFromFloat(ClassNode classNode, MethodNode methodNode) {
        makeIntMethodSymlinkFromFloat(classNode, methodNode, false);
    }

    public static void makeIntMethodSymlinkFromFloat(ClassNode classNode, MethodNode methodNode, boolean returnVoid) {
        Type[] methodArguments = Type.getArgumentTypes(methodNode.desc);
        for (int i = 0;i < methodArguments.length; i++) {
            if (methodArguments[i].getSort() == Type.FLOAT) {
                methodArguments[i] = Type.INT_TYPE;
            }
        }
        Type returnType = Type.getReturnType(methodNode.desc);
        if (returnType.getSort() == Type.VOID) {
            returnVoid = false;
        } else if (returnVoid) {
            returnType = Type.VOID_TYPE;
        }
        String desc = Type.getMethodDescriptor(returnType, methodArguments);
        if (desc.equals(methodNode.desc) || findMethod(classNode, methodNode.name, desc) != null) {
            return;
        }
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
        MethodNode newMethodNode = new MethodNode(
                methodNode.access | Opcodes.ACC_SYNTHETIC,
                methodNode.name, desc, null, null);
        if (!isStatic) {
            newMethodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        Type[] types = Type.getMethodType(methodNode.desc).getArgumentTypes();
        for (int i = 0;i < types.length; i++) {
            if (types[i].getSort() == Type.FLOAT) {
                newMethodNode.instructions.add(new VarInsnNode(
                        Opcodes.ILOAD, i + (isStatic ? 0 : 1)));
                newMethodNode.instructions.add(new InsnNode(Opcodes.I2F));
            } else {
                newMethodNode.instructions.add(new VarInsnNode(
                        types[i].getOpcode(Opcodes.ILOAD), i + (isStatic ? 0 : 1)));
            }
        }
        newMethodNode.instructions.add(new MethodInsnNode(
                isStatic ? Opcodes.INVOKESTATIC : (isInterface ?
                        Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL),
                classNode.name, methodNode.name, methodNode.desc, isInterface));
        if (returnVoid) {
            newMethodNode.instructions.add(new InsnNode(Opcodes.POP));
            newMethodNode.instructions.add(new InsnNode(Opcodes.RETURN));
        } else {
            newMethodNode.instructions.add(new InsnNode(
                    Type.getMethodType(methodNode.desc)
                            .getReturnType().getOpcode(Opcodes.IRETURN)));
        }
        classNode.methods.add(newMethodNode);
    }

    public static @Nullable AbstractInsnNode findLastPutFieldOf(
            ClassNode classNode, MethodNode methodNode, FieldNode fieldNode) {
        AbstractInsnNode lastSet = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.PUTFIELD) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode;
                if (classNode.name.equals(fieldInsnNode.owner) &&
                        fieldNode.name.equals(fieldInsnNode.name) &&
                        fieldNode.desc.equals(fieldInsnNode.desc)) {
                    lastSet = fieldInsnNode;
                }
            }
        }
        return lastSet;
    }

    public static AbstractInsnNode getFirstSwitchInsn(MethodNode methodNode) {
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.LOOKUPSWITCH ||
                    abstractInsnNode.getOpcode() == Opcodes.TABLESWITCH) {
                return abstractInsnNode;
            }
        }
        throw new NoSuchElementException("No switch insn in " + methodNode.name + methodNode.desc);
    }

    public static void insertInDefaultCase(MethodNode methodNode, AbstractInsnNode abstractInsnNode, InsnList newCode) {
        insertInDefaultCase(methodNode, abstractInsnNode, newCode, false);
    }

    public static void insertInDefaultCase(
            MethodNode methodNode, AbstractInsnNode abstractInsnNode, InsnList newCode, boolean forceNewCodeAsTail) {
        LabelNode oldDefaultLabelNode;
        LabelNode newDefaultLabelNode;
        if (abstractInsnNode.getOpcode() == Opcodes.LOOKUPSWITCH) {
            LookupSwitchInsnNode switchInsnNode =
                    (LookupSwitchInsnNode) abstractInsnNode;
            oldDefaultLabelNode = switchInsnNode.dflt;
            newDefaultLabelNode = new LabelNode();
            switchInsnNode.dflt = newDefaultLabelNode;
        } else if (abstractInsnNode.getOpcode() == Opcodes.TABLESWITCH) {
            TableSwitchInsnNode switchInsnNode =
                    (TableSwitchInsnNode) abstractInsnNode;
            oldDefaultLabelNode = switchInsnNode.dflt;
            newDefaultLabelNode = new LabelNode();
            switchInsnNode.dflt = newDefaultLabelNode;
        } else {
            throw new IllegalStateException("Input opcode is not a switch!");
        }
        // forceNewCodeAsTail exist to improve code decompilation results.
        if (forceNewCodeAsTail) {
            InsnList injectedCode = new InsnList();
            injectedCode.add(newDefaultLabelNode);
            injectedCode.add(newCode);
            AbstractInsnNode lastCode = methodNode.instructions.getLast();
            if (lastCode.getOpcode() == -1) {
                lastCode = previousCodeInsn(lastCode);
            }
            methodNode.instructions.insert(lastCode, injectedCode);
            return;
        }
        InsnList injectedCode = new InsnList();
        LabelNode endOfInject = new LabelNode();
        injectedCode.add(new JumpInsnNode(Opcodes.GOTO, endOfInject));
        injectedCode.add(newDefaultLabelNode);
        injectedCode.add(newCode);
        injectedCode.add(endOfInject);
        methodNode.instructions.insert(oldDefaultLabelNode, injectedCode);
    }

    public static void patchInValue$(ClassNode classNode) {
        String desc = "[L" + classNode.name + ";";
        String mDesc = "()[L" + classNode.name + ";";
        if (findMethod(classNode, "values$", mDesc) != null) {
            return; // Skip if it already exists
        }
        MethodNode values = (classNode.access & Opcodes.ACC_ENUM) != 0 ?
                TransformerUtils.getMethod(classNode, "values", mDesc) :
                TransformerUtils.findMethod(classNode, "values", mDesc);
        if (values == null) {
            return; // skip if non enum
        }
        MethodNode values$ = TransformerUtils.copyMethodNode(values);
        values$.name = "values$";
        values$.access |= Opcodes.ACC_SYNTHETIC;
        for (AbstractInsnNode abstractInsnNode : values$.instructions) {
            if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;

                if (desc.equals(methodInsnNode.owner) &&
                        "clone".equals(methodInsnNode.name) &&
                        "()Ljava/lang/Object;".equals(methodInsnNode.desc)) {
                    abstractInsnNode = methodInsnNode.getNext();
                    if (abstractInsnNode.getOpcode() == Opcodes.CHECKCAST &&
                            desc.equals(((TypeInsnNode) abstractInsnNode).desc)) {
                        values$.instructions.remove(methodInsnNode);
                        values$.instructions.remove(abstractInsnNode);
                        break;
                    }
                }
            }
        }
        classNode.methods.add(classNode.methods.indexOf(values) + 1, values$);
    }
}
