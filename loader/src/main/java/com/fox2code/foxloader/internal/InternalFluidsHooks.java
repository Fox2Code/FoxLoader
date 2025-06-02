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
