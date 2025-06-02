package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.*;

final class RenderPatch extends GamePatch {
    private static final String GameRenderer = "net/minecraft/client/renderer/world/GameRenderer";
    private static final String CameraAndRenderUpdatedEvent = "com/fox2code/foxloader/event/client/CameraAndRenderUpdatedEvent";
    private static final String RenderBlocks = "net/minecraft/client/renderer/world/RenderBlocks";
    private static final String RenderEngine = "net/minecraft/client/renderer/world/RenderEngine";
    private static final String BlockAccess = "net/minecraft/common/world/BlockAccess";
    private static final String Block = "net/minecraft/common/block/Block";
    private static final String BlockRenderManager$Internal = "com/fox2code/foxloader/client/BlockRenderManager$Internal";
    private static final String TexturesRefreshEvent = "com/fox2code/foxloader/event/client/TexturesRefreshEvent";
    private static final String TexturesRefreshedEvent = "com/fox2code/foxloader/event/client/TexturesRefreshedEvent";

    RenderPatch() {
        super(new String[]{GameRenderer, RenderBlocks, RenderEngine});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (GameRenderer.equals(classNode.name)) {
            patchGameRenderer(classNode);
        } else if (RenderBlocks.equals(classNode.name)) {
            patchRenderBlocks(classNode);
        } else if (RenderEngine.equals(classNode.name)) {
            patchRenderEngine(classNode);
        }
        return classNode;
    }

    private static void patchGameRenderer(ClassNode classNode) {
        MethodNode updateCameraAndRender = TransformerUtils.getMethod(classNode, "updateCameraAndRender");
        InsnList insnList = new InsnList();
        insnList.add(new FieldInsnNode(GETSTATIC, CameraAndRenderUpdatedEvent,
                "INSTANCE", "L" + CameraAndRenderUpdatedEvent + ";"));
        insnList.add(new VarInsnNode(FLOAD, 1));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL, CameraAndRenderUpdatedEvent, "callEvent", "(F)V"));
        TransformerUtils.insertToEndOfCode(updateCameraAndRender, insnList);
    }

    private static void patchRenderBlocks(ClassNode classNode) {
        MethodNode renderItemIn3d = TransformerUtils.getMethod(classNode, "renderItemIn3d");
        InsnList renderItemIn3dFLX = new InsnList();
        renderItemIn3dFLX.add(new VarInsnNode(ILOAD, 0));
        renderItemIn3dFLX.add(new MethodInsnNode(INVOKESTATIC,
                BlockRenderManager$Internal, "renderItemIn3D", "(I)Z"));
        renderItemIn3dFLX.add(new InsnNode(IRETURN));
        insertDefaultSwitchCodeTail(renderItemIn3d, renderItemIn3dFLX);
        MethodNode renderBlockByRenderType = TransformerUtils.getMethod(classNode, "renderBlockByRenderType");
        FieldNode blockAccess = TransformerUtils.getField(classNode, "blockAccess");
        InsnList renderBlockByRenderTypeFLX = new InsnList();
        renderBlockByRenderTypeFLX.add(new VarInsnNode(ALOAD, 0));
        renderBlockByRenderTypeFLX.add(new FieldInsnNode(GETFIELD,
                RenderBlocks, blockAccess.name, blockAccess.desc));
        renderBlockByRenderTypeFLX.add(new VarInsnNode(ALOAD, 1));
        renderBlockByRenderTypeFLX.add(new VarInsnNode(ILOAD, 2));
        renderBlockByRenderTypeFLX.add(new VarInsnNode(ILOAD, 3));
        renderBlockByRenderTypeFLX.add(new VarInsnNode(ILOAD, 4));
        renderBlockByRenderTypeFLX.add(new VarInsnNode(ILOAD, 5));
        renderBlockByRenderTypeFLX.add(new MethodInsnNode(INVOKESTATIC,
                BlockRenderManager$Internal, "renderBlockByType",
                "(L" + BlockAccess + ";L" + Block + ";IIII)Z"));
        renderBlockByRenderTypeFLX.add(new InsnNode(IRETURN));
        insertDefaultSwitchCodeTail(renderBlockByRenderType, renderBlockByRenderTypeFLX);
    }

    private static void insertDefaultSwitchCodeTail(MethodNode methodNode, InsnList insnList) {
        TransformerUtils.insertInDefaultCase(methodNode,
                TransformerUtils.getFirstSwitchInsn(methodNode), insnList, true);
    }

    private static void patchRenderEngine(ClassNode classNode) {
        MethodNode refreshTextures = TransformerUtils.getMethod(classNode, "refreshTextures");
        InsnList textureRefreshEvent = new InsnList();
        textureRefreshEvent.add(new FieldInsnNode(GETSTATIC,
                TexturesRefreshEvent, "INSTANCE", "L" + TexturesRefreshEvent + ";"));
        textureRefreshEvent.add(new MethodInsnNode(INVOKEVIRTUAL,
                TexturesRefreshEvent, "callEvent", "()V"));
        TransformerUtils.insertToBeginningOfCode(refreshTextures, textureRefreshEvent);
        InsnList textureRefreshedEvent = new InsnList();
        textureRefreshedEvent.add(new FieldInsnNode(GETSTATIC,
                TexturesRefreshedEvent, "INSTANCE", "L" + TexturesRefreshedEvent + ";"));
        textureRefreshedEvent.add(new MethodInsnNode(INVOKEVIRTUAL,
                TexturesRefreshedEvent, "callEvent", "()V"));
        TransformerUtils.insertToEndOfCode(refreshTextures, textureRefreshedEvent);
    }
}
