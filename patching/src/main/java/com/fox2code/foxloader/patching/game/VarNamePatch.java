/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Locale;

final class VarNamePatch extends GamePatch {
    VarNamePatch() {
        super(ALL_CLASSES);
    }

    private static boolean isLocalVariableNodeOk(LocalVariableNode localVariableNode) {
        String varName = localVariableNode.name;
        return (varName.length() < 4 || // Check1, verify if var123 name
                !varName.startsWith("var") || !Character.isDigit(varName.charAt(3))) &&
                // Check2, verify if it is "s" string
                (!(varName.equals("s") && "Ljava/lang/String;".equals(localVariableNode.desc)));
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.localVariables != null) {
                int firstArgIndex = (methodNode.access & ACC_STATIC) == 0 ? 1 : 0;
                int maxArgIndex = (methodNode.access & ACC_STATIC) == 0 ? 1 : 0;
                for (Type arg : Type.getArgumentTypes(methodNode.desc)) {
                    maxArgIndex += arg.getSize();
                }
                HashSet<String> reservedNames = new HashSet<>();
                reservedNames.add("this");
                reservedNames.add("super");
                for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                    if (localVariableNode.index == 0 &&
                            (methodNode.access & ACC_STATIC) == 0) {
                        localVariableNode.name = "this";
                        continue;
                    }
                    if (isLocalVariableNodeOk(localVariableNode)) {
                        reservedNames.add(localVariableNode.name);
                    }
                }
                for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                    if (localVariableNode.index == 0 &&
                            (methodNode.access & ACC_STATIC) == 0) {
                        continue;
                    }
                    if (isLocalVariableNodeOk(localVariableNode)) {
                        continue;
                    }
                    String varName = null;
                    final String desc = localVariableNode.desc;
                    final String signature = localVariableNode.signature;
                    boolean list;
                    if (signature != null && !signature.equals(desc) &&
                            (desc.startsWith("Ljava/util/") ||
                                    desc.startsWith("Lit/unimi/dsi/fastutil/")) &&
                            ((list = desc.endsWith("List;")) || desc.endsWith("Set;"))) {
                        int start = signature.indexOf('<');
                        int end = signature.indexOf('>', start);
                        int type = signature.lastIndexOf('/', end);
                        int subType = signature.lastIndexOf('$', end);
                        if (type < start || type > end) type = start;
                        if (subType > type && subType < end) type = subType;
                        varName = signature.substring(type + 1, type + 2).toLowerCase(Locale.ROOT) +
                                signature.substring(type + 2, end - 1) + (list ? "List" : "Set");
                    } else if (desc.equals("Ljava/lang/Throwable;")) {
                        varName = "t";
                    } else if (desc.equals("Ljava/lang/Exception;")) {
                        varName = "e";
                    } else if (desc.equals("Ljava/io/IOException;")) {
                        varName = "ioe";
                    } else if (localVariableNode.index == firstArgIndex &&
                            desc.equals("Ljava/lang/String;") &&
                            methodNode.name.startsWith("translateKey")) {
                        varName = "key";
                    } else if (desc.startsWith("L") && !desc.startsWith("Ljava/lang/")) {
                        int i = Math.max(desc.lastIndexOf('/') + 1, 1);
                        int subType = desc.indexOf('$', i);
                        if (subType != -1) i = subType + 1;
                        if (desc.charAt(i) == 'I' &&
                                Character.isUpperCase(desc.charAt(i + 1))) {
                            i++;
                        }
                        int uncaps = 0;
                        while (Character.isUpperCase(localVariableNode.desc.charAt(i + uncaps)) && uncaps < desc.length()) {
                            uncaps++;
                        }
                        if (uncaps > 1) uncaps--;
                        varName = localVariableNode.desc.substring(i, i + uncaps).toLowerCase(Locale.ROOT) +
                                localVariableNode.desc.substring(i + uncaps, desc.length() - 1);
                    } else if (desc.equals("[Ljava/lang/Object;")) {
                        if (localVariableNode.index == (maxArgIndex - 1))
                            varName = "args";
                    } else if (desc.startsWith("[L")) {
                        int i = Math.max(desc.lastIndexOf('/') + 1, 2);
                        int subType = desc.indexOf('$', i);
                        if (subType != -1) i = subType + 1;
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
                        localVariableNode.name = varName;
                    } else if (localVariableNode.index < maxArgIndex) {
                        varName = "arg" + localVariableNode.index;
                        if (!reservedNames.contains(varName)) {
                            localVariableNode.name = varName;
                        }
                    }
                    if (!reservedNames.add(localVariableNode.name)) {
                        varName = "var" + localVariableNode.index;
                        if (!reservedNames.add(varName)) {
                            varName = "var_" + localVariableNode.index;
                        }
                        localVariableNode.name = varName;
                    }
                    reservedNames.add(varName);
                }
            }
        }
        return classNode;
    }
}
