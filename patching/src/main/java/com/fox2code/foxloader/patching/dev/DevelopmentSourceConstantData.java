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
package com.fox2code.foxloader.patching.dev;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DevelopmentSourceConstantData {
    private static final int STATUS_DISABLED = -1;
    private static final int STATUS_DEFAULT = 0;
    private static final int STATUS_DISPLAY_FIRST = 1;
    public final String internalVersion, version, displayVersion;
    private final ConstantCheck[] regular, displayFirst;

    private DevelopmentSourceConstantData(ClassNode classNode) {
        this.internalVersion = TransformerUtils.getFieldStringData(classNode, "INTERNAL_VERSION");
        this.version = TransformerUtils.getFieldStringData(classNode, "VERSION");
        this.displayVersion = TransformerUtils.getFieldStringData(classNode, "DISPLAY_VERSION");
        CoreConstantCheck internalVersionCheck = new CoreConstantCheck(this.internalVersion, "INTERNAL_VERSION");
        CoreConstantCheck versionCheck = new CoreConstantCheck(this.version, "VERSION");
        CoreConstantCheck displayVersionCheck = new CoreConstantCheck(this.displayVersion, "DISPLAY_VERSION");
        if (Objects.equals(this.version, this.displayVersion)) {
            this.regular = new ConstantCheck[]{versionCheck, internalVersionCheck};
            this.displayFirst = new ConstantCheck[]{displayVersionCheck, internalVersionCheck};
        } else {
            this.displayFirst = this.regular = new ConstantCheck[]{
                    displayVersionCheck, versionCheck, internalVersionCheck};
        }
    }

    public static DevelopmentSourceConstantData fromPatchedJar(File patchedJar) throws IOException {
        try (JarFile jarFile = new JarFile(patchedJar)) {
            JarEntry jarEntry = jarFile.getJarEntry("net/minecraft/common/CoreConstants.class");
            ClassNode coreConstantsClassNode = new ClassNode();
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                new ClassReader(inputStream).accept(coreConstantsClassNode, ClassReader.SKIP_FRAMES);
            }
            return new DevelopmentSourceConstantData(coreConstantsClassNode);
        }
    }

    public int methodStatus(ClassNode classNode, MethodNode methodNode) {
        switch (classNode.name) {
            case "net/minecraft/common/CoreConstants":
                return STATUS_DISABLED;
            case "net/minecraft/client/Minecraft":
                return "startGame".equals(methodNode.name) ? STATUS_DISPLAY_FIRST : STATUS_DEFAULT;
            case "net/minecraft/client/gui/GuiDebug":
                return "drawScreen".equals(methodNode.name) ? STATUS_DISPLAY_FIRST : STATUS_DEFAULT;
        }
        return STATUS_DEFAULT;
    }

    public void patchStringConstant(IdentityHashMap<AbstractInsnNode, InsnList> constantPatching,
                                    int state, LdcInsnNode ldcInsnNode, ArrayList<AbstractInsnNode> list) {
        if (state == STATUS_DISABLED) return;
        if (!list.isEmpty()) {
            list.clear();
        }
        list.add(ldcInsnNode);
        ConstantCheck[] check = // Some method should prioritize display instead of version.
                state == STATUS_DISPLAY_FIRST ? this.displayFirst : this.regular;
        for (ConstantCheck constantCheck : check) {
            constantCheck.applyCheck(list);
        }
        if (list.size() != 1 || list.get(0).getOpcode() != Opcodes.LDC) {
            constantPatching.put(ldcInsnNode,
                    TransformerUtils.compileStringAppendChain(list));
        }
    }

    private static abstract class ConstantCheck {
        private final String value;

        private ConstantCheck(String value) {
            this.value = value;
        }

        public void applyCheck(ArrayList<AbstractInsnNode> list) {
            for (int listIndex = 0; listIndex < list.size(); listIndex++) {
                AbstractInsnNode abstractInsnNode = list.get(listIndex);
                if (abstractInsnNode.getOpcode() == Opcodes.LDC) {
                    String data = (String) ((LdcInsnNode) abstractInsnNode).cst;
                    int valIndex = data.indexOf(this.value);
                    if (valIndex == -1) continue;
                    if (valIndex == 0) {
                        list.set(listIndex, this.makeFieldInsnNode());
                        if (data.length() != this.value.length()) {
                            list.add(listIndex + 1, new LdcInsnNode(data.substring(this.value.length())));
                        }
                    } else {
                        if (list.size() == 1) {
                            list.set(listIndex, new LdcInsnNode(data.substring(0, valIndex)));
                        } else {
                            ((LdcInsnNode) abstractInsnNode).cst = data.substring(0, valIndex);
                        }
                        list.add(listIndex + 1, this.makeFieldInsnNode());
                        if (data.length() != (valIndex + this.value.length())) {
                            list.add(listIndex + 2, new LdcInsnNode(data.substring(valIndex + this.value.length())));
                        }
                        // skip next instruction as we know it is our field instruction
                        listIndex++;
                    }
                }
            }
        }

        public abstract FieldInsnNode makeFieldInsnNode();
    }

    private static class CoreConstantCheck extends ConstantCheck{
        private final String fieldName;

        private CoreConstantCheck(String value, String fieldName) {
            super(value);
            this.fieldName = fieldName;
        }

        public FieldInsnNode makeFieldInsnNode() {
            return new FieldInsnNode(Opcodes.GETSTATIC,
                    "net/minecraft/common/CoreConstants", this.fieldName, "Ljava/lang/String;");
        }
    }
}
