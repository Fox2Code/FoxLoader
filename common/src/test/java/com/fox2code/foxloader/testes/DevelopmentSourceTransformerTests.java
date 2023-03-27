package com.fox2code.foxloader.testes;

import static com.fox2code.foxloader.loader.transformer.DevelopmentSourceTransformer.*;

import com.fox2code.foxloader.loader.transformer.DevelopmentSourceTransformer;
import com.fox2code.foxloader.loader.transformer.GeneratedConstantUnpicks;
import com.fox2code.foxloader.loader.transformer.TransformerUtils;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class DevelopmentSourceTransformerTests implements Opcodes {
    public DevelopmentSourceTransformerTests() {
        new ParamsConstantUnpick();
    }

    @Test
    public void testParamsUnpickNoop() {
        InsnList insnList = new InsnList();
        StringBuilder testDebug = new StringBuilder();
        AbstractInsnNode constant;
        insnList.add(constant = TransformerUtils.getNumberInsn(2929));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glEnable", "(I)V"));
        insnList.add(new InsnNode(RETURN));
        new ParamsConstantUnpick(GeneratedConstantUnpicks.openGLConstantUnpick)
                .setTestDebug(testDebug).unpick(insnList, constant);
        assertUnpicked(insnList, constant, testDebug);
    }

    @Test
    public void testParamsUnpickMono() {
        InsnList insnList = new InsnList();
        StringBuilder testDebug = new StringBuilder();
        AbstractInsnNode constant;
        AbstractInsnNode ldc;
        insnList.add(constant = TransformerUtils.getNumberInsn(516));
        insnList.add(ldc = new LdcInsnNode(0.1F));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glAlphaFunc", "(IF)V"));
        insnList.add(new InsnNode(RETURN));
        new ParamsConstantUnpick(GeneratedConstantUnpicks.openGLConstantUnpick, null)
                .setTestDebug(testDebug).unpick(insnList, ldc);
        assertUnpicked(insnList, constant, testDebug);
    }

    @Test
    public void testParamsUnpickDuo() {
        InsnList insnList = new InsnList();
        StringBuilder testDebug = new StringBuilder();
        AbstractInsnNode constant0, constant1;
        insnList.add(constant0 = TransformerUtils.getNumberInsn(1032));
        insnList.add(constant1 = TransformerUtils.getNumberInsn(5634));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glColorMaterial", "(II)V"));
        insnList.add(new InsnNode(RETURN));
        new ParamsConstantUnpick(GeneratedConstantUnpicks.openGLConstantUnpick,
                GeneratedConstantUnpicks.openGLConstantUnpick)
                .setTestDebug(testDebug).unpick(insnList, constant1);
        assertUnpicked(insnList, constant0, testDebug);
        assertUnpicked(insnList, constant1, testDebug);
    }

    @Test
    public void testParamsUnpickDuoNested() {
        ClassNode classNode = new ClassNode();
        MethodNode methodNode = new MethodNode();
        classNode.methods.add(methodNode);
        InsnList insnList = methodNode.instructions;
        StringBuilder testDebug = new StringBuilder();
        AbstractInsnNode constant0, constant1;
        insnList.add(constant0 = TransformerUtils.getNumberInsn(1032));
        insnList.add(constant1 = TransformerUtils.getNumberInsn(5634));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glColorMaterial", "(II)V"));
        insnList.add(new InsnNode(RETURN));
        new DevelopmentSourceTransformer(testDebug).transform(classNode, "net.minecraft.Class");
        assertUnpicked(insnList, constant0, testDebug);
        assertUnpicked(insnList, constant1, testDebug);
    }

    @Test
    public void testParamsUnpickBehindMethod() {
        InsnList insnList = new InsnList();
        StringBuilder testDebug = new StringBuilder();
        AbstractInsnNode constant, notConstant;
        insnList.add(constant = TransformerUtils.getNumberInsn(3553));
        insnList.add(new VarInsnNode(ALOAD, 0));
        insnList.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/Minecraft",
                "renderEngine", "Lnet/minecraft/src/client/renderer/RenderEngine;"));
        insnList.add(new LdcInsnNode("/title/mojang.png"));
        insnList.add(notConstant = new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/src/client/renderer/RenderEngine", "getTexture", "(Ljava/lang/String;)I"));
        insnList.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glColorMaterial", "(II)V"));
        insnList.add(new InsnNode(RETURN));
        new ParamsConstantUnpick(GeneratedConstantUnpicks.openGLConstantUnpick, null)
                .setTestDebug(testDebug).unpick(insnList, notConstant);
        assertUnpicked(insnList, constant, testDebug);
    }

    public void assertUnpicked(InsnList insnList, AbstractInsnNode constant, StringBuilder testDebug) {
        if (constant.getNext() != null) {
            StringBuilder stringBuilder = new StringBuilder(256).append("Constant wasn't inlined!\n---\n");
            if (testDebug != null) {
                stringBuilder.append(testDebug).append("\n---\n");
            }
            TransformerUtils.printInsnList(insnList, stringBuilder);
            stringBuilder.append("\n---");
            throw new AssertionError(stringBuilder.toString());
        }
    }
}
