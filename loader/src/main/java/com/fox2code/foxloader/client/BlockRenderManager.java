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
