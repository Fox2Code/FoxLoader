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

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class DevelopmentSourceConstantData {
    private static final int STATUS_DISABLED = -1;
    private static final int STATUS_DEFAULT = 0;
    private static final int STATUS_DISPLAY_FIRST = 1;
    private static final int PUBLIC_STATIC_FINAL = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
    private static final String CoreConstants = "net/minecraft/common/CoreConstants";
    private static final String ChatColors = "net/minecraft/common/util/ChatColors";
    private static final String ASMBuildConfig = "com/fox2code/foxloader/launcher/BuildConfig";
    public final String internalVersion, version, displayVersion;
    private final ConstantCheck[] regular, displayFirst;
    private final ArrayList<ConstantCheck> chatColorsConstantChecks;
    private final HashSet<String> constants;

    private DevelopmentSourceConstantData(ClassNode coreConstantsClassNode, ClassNode chatColorsClassNode) {
        this.internalVersion = TransformerUtils.getFieldStringData(coreConstantsClassNode, "INTERNAL_VERSION");
        this.version = TransformerUtils.getFieldStringData(coreConstantsClassNode, "VERSION");
        this.displayVersion = TransformerUtils.getFieldStringData(coreConstantsClassNode, "DISPLAY_VERSION");
        CoreConstantCheck internalVersionCheck = new CoreConstantCheck(this.internalVersion, "INTERNAL_VERSION");
        CoreConstantCheck versionCheck = new CoreConstantCheck(this.version, "VERSION");
        CoreConstantCheck displayVersionCheck = new CoreConstantCheck(this.displayVersion, "DISPLAY_VERSION");
        FLConstantCheck flVersionCheck = new FLConstantCheck(BuildConfig.FOXLOADER_DISPLAY, "FOXLOADER_DISPLAY");
        if (Objects.equals(this.version, this.displayVersion)) {
            this.regular = new ConstantCheck[]{versionCheck, internalVersionCheck, flVersionCheck};
            this.displayFirst = new ConstantCheck[]{displayVersionCheck, internalVersionCheck, flVersionCheck};
        } else {
            this.displayFirst = this.regular = new ConstantCheck[]{
                    displayVersionCheck, versionCheck, internalVersionCheck, flVersionCheck};
        }
        this.chatColorsConstantChecks = new ArrayList<>();
        this.constants = new HashSet<>();
        this.constants.add(this.internalVersion);
        this.constants.add(this.version);
        this.constants.add(this.displayVersion);
        this.constants.add(BuildConfig.FOXLOADER_DISPLAY);

        for (FieldNode fieldNode : chatColorsClassNode.fields) {
            if ((fieldNode.access & PUBLIC_STATIC_FINAL) == PUBLIC_STATIC_FINAL &&
                    fieldNode.value != null && fieldNode.desc.equals("Ljava/lang/String;")) {
                String value = (String) fieldNode.value;
                if (value.length() != 2) continue;
                if (this.constants.add(value)) {
                    this.chatColorsConstantChecks.add(
                            new ColorConstantCheck(value, fieldNode.name));
                }
            }
        }
    }

    public static DevelopmentSourceConstantData fromPatchedJar(File patchedJar) throws IOException {
        try (JarFile jarFile = new JarFile(patchedJar)) {
            JarEntry coreConstantsJarEntry = jarFile.getJarEntry(CoreConstants + ".class");
            ClassNode coreConstantsClassNode = new ClassNode();
            try (InputStream inputStream = jarFile.getInputStream(coreConstantsJarEntry)) {
                new ClassReader(inputStream).accept(coreConstantsClassNode, ClassReader.SKIP_FRAMES);
            }
            JarEntry chatColorsJarEntry = jarFile.getJarEntry(ChatColors + ".class");
            ClassNode chatColorsClassNode = new ClassNode();
            try (InputStream inputStream = jarFile.getInputStream(chatColorsJarEntry)) {
                new ClassReader(inputStream).accept(chatColorsClassNode, ClassReader.SKIP_FRAMES);
            }
            return new DevelopmentSourceConstantData(coreConstantsClassNode, chatColorsClassNode);
        }
    }

    public List<ConstantCheck> getClassConstantChecks(ClassNode classNode) {
        if (CoreConstants.equals(classNode.name) || ChatColors.equals(classNode.name)) {
            return Collections.emptyList();
        }
        ArrayList<ConstantCheck> constantChecks = null;
        HashSet<String> usedConstants = null;
        for (FieldNode fieldNode : classNode.fields) {
            if (((fieldNode.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) ==
                    (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) && fieldNode.value != null &&
                    "Ljava/lang/String;".equals(fieldNode.desc)) {
                String value = (String) fieldNode.value;
                if (value.length() <= 1) continue;
                if (usedConstants == null) {
                    usedConstants = new HashSet<>(this.constants);
                }
                if (usedConstants.add(value)) {
                    if (constantChecks == null) constantChecks = new ArrayList<>();
                    constantChecks.add(new ClassConstantCheck(value, classNode.name, fieldNode.name));
                } else {
                    System.out.println("Duplicate constant in " + classNode.name + " for \"" + value + "\"");
                }
            }
        }

        return constantChecks == null ? Collections.emptyList() : constantChecks;
    }

    public int methodStatus(ClassNode classNode, MethodNode methodNode) {
        switch (classNode.name) {
            case CoreConstants:
                return STATUS_DISABLED;
            case "net/minecraft/client/Minecraft":
                return "startGame".equals(methodNode.name) ? STATUS_DISPLAY_FIRST : STATUS_DEFAULT;
            case "net/minecraft/client/gui/GuiDebug":
                return "drawScreen".equals(methodNode.name) ? STATUS_DISPLAY_FIRST : STATUS_DEFAULT;
        }
        return STATUS_DEFAULT;
    }

    public void patchStringConstant(IdentityHashMap<AbstractInsnNode, InsnList> constantPatching,
                                    List<ConstantCheck> classConstantCheck, int state,
                                    LdcInsnNode ldcInsnNode, ArrayList<AbstractInsnNode> list) {
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
        for (ConstantCheck constantCheck : classConstantCheck) {
            constantCheck.applyCheck(list);
        }
        for (ConstantCheck constantCheck : this.chatColorsConstantChecks) {
            constantCheck.applyCheck(list);
        }
        if (list.size() != 1 || list.get(0).getOpcode() != Opcodes.LDC) {
            constantPatching.put(ldcInsnNode,
                    TransformerUtils.compileStringAppendChain(list));
        }
    }

    public static abstract class ConstantCheck {
        private final String value;

        private ConstantCheck(String value) {
            this.value = value;
        }

        public final String getValue() {
            return this.value;
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

    private static class CoreConstantCheck extends ConstantCheck {
        private final String fieldName;

        private CoreConstantCheck(String value, String fieldName) {
            super(value);
            this.fieldName = fieldName;
        }

        public FieldInsnNode makeFieldInsnNode() {
            return new FieldInsnNode(Opcodes.GETSTATIC,
                    CoreConstants, this.fieldName, "Ljava/lang/String;");
        }
    }

    private static class FLConstantCheck extends ConstantCheck {
        private final String fieldName;

        private FLConstantCheck(String value, String fieldName) {
            super(value);
            this.fieldName = fieldName;
        }

        public FieldInsnNode makeFieldInsnNode() {
            return new FieldInsnNode(Opcodes.GETSTATIC,
                    ASMBuildConfig, this.fieldName, "Ljava/lang/String;");
        }
    }

    private static class ColorConstantCheck extends ConstantCheck {
        private final String fieldName;

        private ColorConstantCheck(String value, String fieldName) {
            super(value);
            this.fieldName = fieldName;
        }

        public FieldInsnNode makeFieldInsnNode() {
            return new FieldInsnNode(Opcodes.GETSTATIC,
                    ChatColors, this.fieldName, "Ljava/lang/String;");
        }
    }

    private static class ClassConstantCheck extends ConstantCheck {
        private final String className;
        private final String fieldName;

        private ClassConstantCheck(String value, String className, String fieldName) {
            super(value);
            this.className = className;
            this.fieldName = fieldName;
        }

        @Override
        public FieldInsnNode makeFieldInsnNode() {
            return new FieldInsnNode(Opcodes.GETSTATIC,
                    this.className, this.fieldName, "Ljava/lang/String;");
        }
    }
}
