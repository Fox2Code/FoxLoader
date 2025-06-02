package com.fox2code.foxloader.client;

import net.minecraft.common.block.Block;
import net.minecraft.common.world.BlockAccess;

public final class BlockRenderManager {
    private static final int FL_INITIAL_ID = 256;
    private static final int FL_MAX_RENDER_TYPES = 1024;
    private static final int FL_MAX_ID = FL_INITIAL_ID + FL_MAX_RENDER_TYPES - 1;
    private static final BlockRender[] blockRenders = new BlockRender[FL_MAX_RENDER_TYPES];
    private static int nextID = FL_INITIAL_ID;

    private BlockRenderManager() {}

    public static void registerBlockRender(BlockRender blockRender) {
        if (blockRender.assignedID != -1) {
            return;
        }
        synchronized (blockRenders) {
            if (blockRender.assignedID != -1) {
                return;
            }
            int newAssignedID = nextID++;
            if (newAssignedID >= FL_MAX_ID) {
                throw new IllegalStateException("Max number of renders registered!");
            }
            blockRenders[newAssignedID] = blockRender;
            blockRender.assignedID = newAssignedID;
        }
    }

    public static final class Internal {
        public static boolean renderBlockByType(BlockAccess blockAccess, Block block, int x, int y, int z, int renderType) {
            if (renderType < FL_INITIAL_ID || renderType > FL_MAX_ID) {
                return false;
            }
            BlockRender blockRender = blockRenders[renderType - FL_INITIAL_ID];
            return blockRender != null && blockRender.renderBlock(blockAccess, block, x, y, z);
        }

        public static boolean renderItemIn3D(int renderType) {
            if (renderType < FL_INITIAL_ID || renderType > FL_MAX_ID) {
                return false;
            }
            BlockRender blockRender = blockRenders[renderType - FL_INITIAL_ID];
            return blockRender != null && blockRender.renderItemIn3D;
        }
    }
}
