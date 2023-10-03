package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class ConsoleLogManagerTransformer implements PreClassTransformer {
    @Override
    public void transform(ClassNode classNode, String className) {
        if (!"net.minecraft.src.server.ConsoleLogManager".equals(className)) return;
        MethodNode methodNode = TransformerUtils.findMethod(classNode, "init");
        if (methodNode == null) return;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == RETURN) {
                InsnList insnList = new InsnList();
                insnList.add(new FieldInsnNode(GETSTATIC,
                        "net/minecraft/src/server/ConsoleLogManager",
                        "logger", "Ljava/util/logging/Logger;"));
                insnList.add(new MethodInsnNode(INVOKESTATIC,
                        "com/fox2code/foxloader/launcher/FoxLauncher",
                        "installLoggerHelperOn", "(Ljava/util/logging/Logger;)V", false));
                methodNode.instructions.insertBefore(abstractInsnNode, insnList);
            }
        }
    }
}
