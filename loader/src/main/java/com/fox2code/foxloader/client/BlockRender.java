package com.fox2code.foxloader.client;

import net.minecraft.common.block.Block;
import net.minecraft.common.world.BlockAccess;

public abstract class BlockRender {
    public final boolean renderItemIn3D;
    int assignedID = -1;

    public BlockRender(boolean renderItemIn3D) {
        this.renderItemIn3D = renderItemIn3D;
    }

    public abstract boolean renderBlock(BlockAccess blockAccess, Block block, int x, int y, int z);

    public final int getAssignedID() {
        return this.assignedID;
    }
}
