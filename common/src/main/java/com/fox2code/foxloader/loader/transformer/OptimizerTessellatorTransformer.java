package com.fox2code.foxloader.loader.transformer;

import org.objectweb.asm.tree.*;

public class OptimizerTessellatorTransformer implements PreClassTransformer {
    private static final String TESSELLATOR = "net/minecraft/src/client/renderer/Tessellator";

    @Override
    public void transform(ClassNode classNode, String className) {
        if (!"net.minecraft.src.client.renderer.Tessellator".equals(className)) return;
        TransformerUtils.getField(classNode, "textureU").value = -1;
        TransformerUtils.getField(classNode, "textureV").value = -1;
        InsnList insnList = new InsnList();
        LabelNode notMatch = new LabelNode();
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, TESSELLATOR, "textureU", "D"));
        insnList.add(new VarInsnNode(DLOAD, 1));
        insnList.add(new InsnNode(DCMPL));
        insnList.add(new JumpInsnNode(IFNE, notMatch));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, TESSELLATOR, "textureV", "D"));
        insnList.add(new VarInsnNode(DLOAD, 3));
        insnList.add(new InsnNode(DCMPL));
        insnList.add(new JumpInsnNode(IFNE, notMatch));
        insnList.add(new InsnNode(RETURN));
        insnList.add(notMatch);
        MethodNode setTextureUV =TransformerUtils.getMethod(
                classNode, "setTextureUV", "(DD)V");
        AbstractInsnNode abstractInsnNode = setTextureUV.instructions.getFirst();
        while (abstractInsnNode.getOpcode() == -1) {
            abstractInsnNode = abstractInsnNode.getNext();
            if (abstractInsnNode == null) return;
        }
        setTextureUV.instructions.insertBefore(abstractInsnNode, insnList);
    }
}
