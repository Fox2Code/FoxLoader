package com.fox2code.foxloader.event.world;

import com.fox2code.foxevents.Event;
import net.minecraft.common.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

@Event.DelegateEvent
public abstract class WorldMultiBlockChange extends WorldChangeEvent
        implements Event.Cancellable, Iterable<WorldMultiBlockChange.BlockChange> {
    private static final BlockChange[] NO_BLOCK_CHANGES = new BlockChange[0];
    private final BlockChange[] blockChanges;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    public WorldMultiBlockChange(World world, Collection<BlockChange> blockChangeList) {
        super(world);
        this.blockChanges = blockChangeList.toArray(NO_BLOCK_CHANGES);
        if (this.blockChanges.length == 0)
            throw new IllegalArgumentException("Need at least one block changed");
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockChange blockChange : this.blockChanges) {
            minX = Math.min(minX, blockChange.x);
            maxX = Math.max(maxX, blockChange.x);
            minY = Math.min(minY, blockChange.y);
            maxY = Math.max(maxY, blockChange.y);
            minZ = Math.min(minZ, blockChange.z);
            maxZ = Math.max(maxZ, blockChange.z);
        }
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    @Override
    public boolean doesChangeBlock(int x, int y, int z) {
        for (BlockChange blockChange : this.blockChanges) {
            if (blockChange.x == x && blockChange.y == y && blockChange.z == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doesChangeBlockInArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (BlockChange blockChange : this.blockChanges) {
            if (Math.min(x1, x2) <= blockChange.x && Math.max(x1, x2) >= blockChange.x &&
                    Math.min(y1, y2) <= blockChange.y && Math.max(y1, y2) >= blockChange.y &&
                    Math.min(z1, z2) <= blockChange.z && Math.max(z1, z2) >= blockChange.z) {
                return true;
            }
        }
        return false;
    }

    public final void applyChanges() {
        if (this.isCancelled()) return;
        final World world = this.getWorld();
        for (BlockChange blockChange : this.blockChanges) {
            if (blockChange.cancelled) continue;
            world.setBlockAndMetadataWithNotify(
                    blockChange.x, blockChange.y, blockChange.z,
                    blockChange.newId, blockChange.newMeta);
        }
    }

    @Override
    public int getBlockChangeMinX() {
        return this.minX;
    }

    @Override
    public int getBlockChangeMaxX() {
        return this.maxX;
    }

    @Override
    public int getBlockChangeMinY() {
        return this.minY;
    }

    @Override
    public int getBlockChangeMaxY() {
        return this.maxY;
    }

    @Override
    public int getBlockChangeMinZ() {
        return this.minZ;
    }

    @Override
    public int getBlockChangeMaxZ() {
        return this.maxZ;
    }

    @Override
    public final @NotNull Iterator<BlockChange> iterator() {
        return Arrays.asList(this.blockChanges).iterator();
    }

    @Override
    public final void forEach(Consumer<? super BlockChange> action) {
        for (BlockChange blockChange : this.blockChanges) {
            action.accept(blockChange);
        }
    }

    @Override
    public final Spliterator<BlockChange> spliterator() {
        return Arrays.spliterator(this.blockChanges);
    }

    public static final class BlockChange {
        public final int x, y, z;
        public int newId, newMeta;
        public boolean cancelled;

        public BlockChange(int x, int y, int z) {
            this(x, y, z, 0, 0);
        }

        public BlockChange(int x, int y, int z, int newId, int newMeta) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.newId = newId;
            this.newMeta = newMeta;
        }

        public boolean equals(Object other) {
            if (!(other instanceof BlockChange)) {
                return false;
            } else {
                BlockChange chunkCoordinates = (BlockChange)other;
                return this.x == chunkCoordinates.x && this.y == chunkCoordinates.y && this.z == chunkCoordinates.z;
            }
        }

        public int hashCode() {
            return this.x + this.z << 8 + this.y << 16;
        }
    }
}