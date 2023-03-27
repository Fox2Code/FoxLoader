package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Locale;

public class VarNameTransformer implements PreClassTransformer {
    @Override
    public void transform(ClassNode classNode, String className) {
        if (!className.startsWith("net.minecraft.")) return;
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.localVariables != null) {
                int maxArgIndex = (methodNode.access & ACC_STATIC) == 0 ? 1 : 0;
                for (Type arg : Type.getArgumentTypes(methodNode.desc)) {
                    maxArgIndex += arg.getSize();
                }
                HashSet<String> reservedNames = new HashSet<>();
                reservedNames.add("this");
                for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                    if (localVariableNode.index == 0 &&
                            (methodNode.access & ACC_STATIC) == 0) {
                        localVariableNode.name = "this";
                        continue;
                    }
                    reservedNames.add(localVariableNode.name);
                }
                for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                    if (localVariableNode.index == 0 &&
                            (methodNode.access & ACC_STATIC) == 0) {
                        continue;
                    }
                    String varName = localVariableNode.name;
                    if (varName.length() < 4 ||
                            !varName.startsWith("var") ||
                            !Character.isDigit(varName.charAt(3))) {
                        continue;
                    }
                    varName = null;
                    final String desc = localVariableNode.desc;
                    if (desc.startsWith("L") && !desc.startsWith("Ljava/lang/")) {
                        int i = Math.max(desc.lastIndexOf('/') + 1, 1);
                        if (desc.charAt(i) == 'I' &&
                                Character.isUpperCase(desc.charAt(i + 1))) {
                            i++;
                        }
                        varName = localVariableNode.desc.substring(i, i + 1).toLowerCase(Locale.ROOT) +
                                localVariableNode.desc.substring(i + 1, desc.length() - 1);
                    } else if (desc.equals("[Ljava/lang/Object;")) {
                        if (localVariableNode.index == (maxArgIndex - 1))
                            varName = "args";
                    } else if (desc.startsWith("[L")) {
                        int i = Math.max(desc.lastIndexOf('/') + 1, 2);
                        if (desc.charAt(i) == 'I' &&
                                Character.isUpperCase(desc.charAt(i + 1))) {
                            i++;
                        }
                        varName = localVariableNode.desc.substring(i, i + 1).toLowerCase(Locale.ROOT) +
                                localVariableNode.desc.substring(i + 1, desc.length() - 1);
                        if (varName.endsWith("s")) {
                            if (!varName.endsWith("es")) {
                                varName += "es";
                            }
                        } else {
                            varName += "s";
                        }
                    }
                    if (varName != null && !reservedNames.contains(varName)) {
                        reservedNames.add(varName);
                        localVariableNode.name = varName;
                    } else if (localVariableNode.index < maxArgIndex) {
                        varName = "arg" + localVariableNode.index;
                        if (!reservedNames.contains(varName)) {
                            reservedNames.add(varName);
                            localVariableNode.name = varName;
                        }
                    }
                }
            }
        }
    }
}
