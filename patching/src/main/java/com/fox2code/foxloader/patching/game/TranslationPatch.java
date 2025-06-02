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
