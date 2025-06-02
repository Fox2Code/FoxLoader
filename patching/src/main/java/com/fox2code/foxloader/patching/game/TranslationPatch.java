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
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public final class TranslationPatch extends GamePatch {
    private static final String StringTranslate = "net/minecraft/common/util/i18n/StringTranslate";
    private static final String InternalTranslateHooks = "com/fox2code/foxloader/internal/InternalTranslateHooks";

    TranslationPatch() {
        super(StringTranslate);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        InsnList insnList = new InsnList();
        insnList.add(new FieldInsnNode(GETSTATIC, StringTranslate, "langFile", "Ljava/lang/String;"));
        insnList.add(new LdcInsnNode("en_US"));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
        LabelNode label1 = new LabelNode();
        insnList.add(new JumpInsnNode(IFNE, label1));
        insnList.add(new FieldInsnNode(GETSTATIC, StringTranslate, "langFile", "Ljava/lang/String;"));
        insnList.add(new MethodInsnNode(INVOKESTATIC, InternalTranslateHooks, "getTranslationsForLanguage", "(Ljava/lang/String;)Ljava/util/Properties;", false));
        insnList.add(new FieldInsnNode(GETSTATIC, StringTranslate, "translateTable", "Ljava/util/Properties;"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false));
        insnList.add(new InsnNode(POP));
        insnList.add(new InvokeDynamicInsnNode("accept", "(Ljava/util/Properties;)Ljava/util/function/BiConsumer;",
                new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", LAMBDA_ARGS, false),
                new Object[]{Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)V"),
                        new Handle(H_INVOKEVIRTUAL, "java/util/Hashtable", "putIfAbsent", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false),
                        Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)V")}));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Properties", "forEach", "(Ljava/util/function/BiConsumer;)V", false));
        insnList.add(label1);
        insnList.add(new FieldInsnNode(GETSTATIC, InternalTranslateHooks, "fallbackTranslations", "Ljava/util/Properties;"));
        insnList.add(new FieldInsnNode(GETSTATIC, StringTranslate, "translateTable", "Ljava/util/Properties;"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false));
        insnList.add(new InsnNode(POP));
        insnList.add(new InvokeDynamicInsnNode("accept", "(Ljava/util/Properties;)Ljava/util/function/BiConsumer;",
                new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", LAMBDA_ARGS, false),
                new Object[]{Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)V"),
                        new Handle(H_INVOKEVIRTUAL, "java/util/Hashtable", "putIfAbsent", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false),
                        Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)V")}));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Properties", "forEach", "(Ljava/util/function/BiConsumer;)V", false));
        MethodNode reloadKeys = TransformerUtils.getMethod(classNode, "reloadKeys");
        TransformerUtils.insertToEndOfCode(reloadKeys, insnList);
        return classNode;
    }
}
