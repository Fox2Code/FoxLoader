package com.fox2code.foxloader.loader.transformer;

import com.fox2code.foxloader.launcher.ClassTransformer;
import org.objectweb.asm.tree.*;

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

    public static MethodNode getMethod(ClassNode classNode, String methodName) {
        return getMethod(classNode, methodName, null);
    }

    public static MethodNode getMethod(ClassNode classNode, String methodName, String methodDesc) {
        for (MethodNode methodNode:classNode.methods) {
            if (methodNode.name.equals(methodName)
                    && (methodDesc == null || methodNode.desc.equals(methodDesc))) {
                return methodNode;
            }
        }
        throw new NoSuchElementException(classNode.name + "." +
                methodName + (methodDesc == null ? "()" : methodDesc));
    }

    public static FieldNode getField(ClassNode classNode,String fieldName) {
        for (FieldNode fieldNode:classNode.fields) {
            if (fieldNode.name.equals(fieldName)) {
                return fieldNode;
            }
        }
        throw new NoSuchElementException(classNode.name + "." + fieldName);
    }
}
