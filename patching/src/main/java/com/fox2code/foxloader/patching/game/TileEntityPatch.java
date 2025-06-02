package com.fox2code.foxloader.patching.game;

import com.fox2code.foxloader.patching.TransformerUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

final class TileEntityPatch extends GamePatch {
    private static final String TileEntity = "net/minecraft/common/block/tileentity/TileEntity";

    TileEntityPatch() {
        super(new String[]{TileEntity});
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        if (TileEntity.equals(classNode.name)) {
            patchTileEntity(classNode);
        }
        return classNode;
    }

    private static void patchTileEntity(ClassNode classNode) {
        MethodNode addMapping = TransformerUtils.getMethod(classNode, "addMapping");
        addMapping.access &= ~(ACC_PRIVATE | ACC_PROTECTED);
        addMapping.access |= ACC_PUBLIC;
    }
}
