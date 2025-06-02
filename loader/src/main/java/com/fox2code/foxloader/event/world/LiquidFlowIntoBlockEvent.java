package com.fox2code.foxloader.event.world;

import com.fox2code.foxevents.Event;
import net.minecraft.common.block.children.BlockFluid;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.Nullable;

@Event.DelegateEvent
public final class LiquidFlowIntoBlockEvent extends WorldChangeEvent.SingleBlockChange implements Event.Cancellable {
    private final BlockFluid blockFluid;
    private final int metadata;

    public LiquidFlowIntoBlockEvent(World world, int x, int y, int z, BlockFluid blockFluid, int metadata) {
        super(world, x, y, z);
        this.blockFluid = blockFluid;
        this.metadata = metadata;
    }

    @Override
    public @Nullable Entity getEntitySource() {
        return null;
    }

    public BlockFluid getBlockFluid() {
        return this.blockFluid;
    }

    public int getMetadata() {
        return this.metadata;
    }
}
