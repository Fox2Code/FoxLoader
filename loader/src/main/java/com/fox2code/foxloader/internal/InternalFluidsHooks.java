/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.world.LiquidFlowIntoBlockEvent;
import net.minecraft.common.block.children.BlockFluid;
import net.minecraft.common.world.World;

public final class InternalFluidsHooks {
    private static final EventHolder<LiquidFlowIntoBlockEvent> LIQUID_FLOW_INTO_BLOCK_EVENT_EVENT =
            EventHolder.getHolderFromEvent(LiquidFlowIntoBlockEvent.class);

    private InternalFluidsHooks() {}

    public static boolean onLiquidFlowIntoBlock(
            World world, int x, int y, int z, BlockFluid blockFluid, int metadata) {
        if (LIQUID_FLOW_INTO_BLOCK_EVENT_EVENT.isEmpty()) return false;
        LiquidFlowIntoBlockEvent liquidFlowIntoBlockEvent =
                new LiquidFlowIntoBlockEvent(world, x, y, z, blockFluid, metadata);
        LIQUID_FLOW_INTO_BLOCK_EVENT_EVENT.callEvent(liquidFlowIntoBlockEvent);
        return liquidFlowIntoBlockEvent.isCancelled();
    }
}
