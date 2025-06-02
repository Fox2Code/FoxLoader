package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

import java.util.Objects;

final class KeyBindingPatch extends GamePatch {
    private static final String KeyBindingAPI$Internal = "com/fox2code/foxloader/client/KeyBindingAPI$Internal";
    private static final String GameSettings = "net/minecraft/client/util/GameSettings";
    private static final String KeyBinding = "net/minecraft/client/util/KeyBinding";

    KeyBindingPatch() {
        super(GameSettings);
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (!GameSettings.equals(classNode.name)) return classNode;
        MethodNode methodNode = TransformerUtils.getMethod(classNode, "init");
        AbstractInsnNode returnInsn = null;
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (abstractInsnNode.getOpcode() == RETURN) {
                returnInsn = abstractInsnNode;
                break;
            }
        }
        Objects.requireNonNull(returnInsn);
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, GameSettings,
                "keyBindings", "[L" + KeyBinding + ";"));
        insnList.add(new MethodInsnNode(INVOKESTATIC, KeyBindingAPI$Internal,
                "inject", "([L" + KeyBinding + ";)[L" + KeyBinding + ";"));
        insnList.add(new FieldInsnNode(PUTFIELD, GameSettings,
                "keyBindings", "[L" + KeyBinding + ";"));
        methodNode.instructions.insertBefore(returnInsn, insnList);
        return classNode;
    }
}
