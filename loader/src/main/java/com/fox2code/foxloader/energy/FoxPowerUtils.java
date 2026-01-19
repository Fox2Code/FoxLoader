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
package com.fox2code.foxloader.energy;

import com.fox2code.foxloader.loader.ModLoaderOptions;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.common.block.Block;
import net.minecraft.common.block.Blocks;
import net.minecraft.common.item.Item;
import net.minecraft.common.item.ItemStack;
import net.minecraft.common.util.Direction;
import net.minecraft.common.util.i18n.StringTranslate;
import net.minecraft.common.world.BlockAccess;
import net.minecraft.common.world.chunk.ChunkCoordIntTrio;

import java.text.DecimalFormat;

public final class FoxPowerUtils {
    private static final DecimalFormat DECIMAL_FORMAT_LOW_FE = new DecimalFormat("#.##");
    private static final DecimalFormat DECIMAL_FORMAT_HIGH_FE = new DecimalFormat("##.#");
    static int maxSinkPriorityValue = 256;

    private FoxPowerUtils() {}

    public static int getMaxSinkPriorityValue() {
        return maxSinkPriorityValue;
    }

    /**
     * Try to insert energy into an item
     *
     * @param itemStack the item to add energy to
     * @param amount the amount of energy to add.
     * @return the amount of energy added
     */
    public static long tryDumpEnergyIntoItem(ItemStack itemStack, long amount) {
        if (amount <= 0) return 0;
        Item item = itemStack.getItem();
        if (!(item instanceof FoxPowerItem)) {
            return 0;
        }
        return ((FoxPowerItem) item).sendFoxPower(itemStack, amount);
    }

    /**
     * Try to remove energy from an item
     *
     * @param itemStack the item to drain energy from
     * @param amount the amount of energy to drain.
     * @return the amount of energy drained
     */
    public static long tryDrainEnergyFromItem(ItemStack itemStack, long amount) {
        if (amount <= 0) return 0;
        Item item = itemStack.getItem();
        if (!(item instanceof FoxPowerItem)) {
            return 0;
        }
        return ((FoxPowerItem) item).drainFoxPower(itemStack, amount);
    }

    /**
     * Get the power interface at a specific block coordinates, but may load chunks,
     * use {@link #getLoadedPowerInterface(BlockAccess, int, int, int, int)} if you do not wish to load chunks.
     *
     * @param blockAccess the block access
     * @param x the x block pos coordinate
     * @param y the y block pos coordinate
     * @param z the z block pos coordinate
     * @param blockFace the blockFace to get a FoxPowerInterface from
     * @return the {@link FoxPowerInterface} for this block
     */
    public static FoxPowerInterface getPowerInterface(
            BlockAccess blockAccess, int x, int y, int z, int blockFace) {
        int blockId = blockAccess.getBlockId(x, y, z);
        if (blockId == 0) return null;
        Block block = Blocks.BLOCKS_LIST[blockId];
        if (!(block instanceof FoxPowerBlock)) {
            return null;
        }
        return ((FoxPowerBlock) block).getPowerInterfaceForFace(blockAccess, x, y, z, blockFace);
    }

    /**
     * Get the power interface at a specific block coordinates, without loading any chunks,
     * use {@link #getPowerInterface(BlockAccess, int, int, int, int)} if you want to load chunks.
     *
     * @param blockAccess the block access
     * @param x the x block pos coordinate
     * @param y the y block pos coordinate
     * @param z the z block pos coordinate
     * @param blockFace the blockFace to get a FoxPowerInterface from
     * @return the {@link FoxPowerInterface} for this block
     */
    public static FoxPowerInterface getLoadedPowerInterface(
            BlockAccess blockAccess, int x, int y, int z, int blockFace) {
        int blockId = blockAccess.getLoadedBlockIdOrM1(x, y, z);
        if (blockId == 0 || blockId == -1) return null;
        Block block = Blocks.BLOCKS_LIST[blockId];
        if (!(block instanceof FoxPowerBlock)) {
            return null;
        }
        return ((FoxPowerBlock) block).getPowerInterfaceForFace(blockAccess, x, y, z, blockFace);
    }

    /**
     * Notify neighboring power blocks that a power block state updated
     *
     * @param blockAccess the block access
     * @param x the x block pos coordinate
     * @param y the y block pos coordinate
     * @param z the z block pos coordinate
     */
    public static void notifyNeighboringPowerBlocks(BlockAccess blockAccess, int x, int y, int z) {
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        for (Direction.EnumDirection enumDirection : Direction.EnumDirection.VALID_DIRECTIONS) {
            int offX = x + enumDirection.offsX;
            int offY = y + enumDirection.offsY;
            int offZ = z + enumDirection.offsZ;
            int blockId = blockAccess.getBlockId(offX, offY, offZ);
            if (blockId == 0 || blockId == -1) continue;
            Block block = Blocks.BLOCKS_LIST[blockId];
            if (!(block instanceof FoxPowerBlock)) {
                continue;
            }
            final int blockFaceScan = enumDirection.ordinal() ^ 1;
            if (((FoxPowerBlock) block).notifyPowerInterfaceForFace(
                    blockAccess, offX, offY, offZ, blockFaceScan)) {
                longOpenHashSet.add(ChunkCoordIntTrio.chunkXYZ2Int(offX, offY, offZ));
            }
        }
        if (!longOpenHashSet.isEmpty()) {
            propagateFoxPowerSinkWithSteps(blockAccess, longOpenHashSet);
        }
    }

    private static void propagateFoxPowerSinkWithSteps(BlockAccess blockAccess, LongOpenHashSet primary) {
        int loopsLeft = FoxPowerUtils.maxSinkPriorityValue; // <- Safety anti-softlock
        LongOpenHashSet secondary = new LongOpenHashSet();
        while (!primary.isEmpty()) {
            if (loopsLeft--==0) {
                throw new RuntimeException("Anti softlock, something went terribly wrong!");
            }
            LongIterator longIterator = primary.longIterator();
            while (longIterator.hasNext()) {
                long next = longIterator.nextLong();
                int x = ChunkCoordIntTrio.unpackX(next);
                int y = ChunkCoordIntTrio.unpackY(next);
                int z = ChunkCoordIntTrio.unpackZ(next);
                for (Direction.EnumDirection enumDirection : Direction.EnumDirection.VALID_DIRECTIONS) {
                    int offX = x + enumDirection.offsX;
                    int offY = y + enumDirection.offsY;
                    int offZ = z + enumDirection.offsZ;
                    int blockId = blockAccess.getBlockId(offX, offY, offZ);
                    if (blockId == 0 || blockId == -1) continue;
                    Block block = Blocks.BLOCKS_LIST[blockId];
                    if (!(block instanceof FoxPowerBlock)) {
                        continue;
                    }
                    final int blockFaceScan = enumDirection.ordinal() ^ 1;
                    if (((FoxPowerBlock) block).notifyPowerInterfaceForFace(
                            blockAccess, offX, offY, offZ, blockFaceScan)) {
                        // Use the assumption that (x + y + z) % 2 is always equals to 0 or 1 for primary
                        secondary.add(ChunkCoordIntTrio.chunkXYZ2Int(offX, offY, offZ));
                    }
                }
            }
            // Swap both sets
            LongOpenHashSet tmp = primary;
            primary = secondary;
            secondary = tmp;
            tmp.clear();
        }
    }

    public static long tryDrainEnergyFromBlock(
            BlockAccess blockAccess, int x, int y, int z, int fromBlockFace, long amount) {
        if (amount <= 0) return 0;
        FoxPowerInterface powerInterface = getLoadedPowerInterface(blockAccess, x, y, z, fromBlockFace);
        return powerInterface == null ? 0 : powerInterface.drainFoxPower(amount);
    }

    public static long trySendEnergyToBlock(
            BlockAccess blockAccess, int x, int y, int z, int fromBlockFace, long amount) {
        if (amount <= 0) return 0;
        FoxPowerInterface powerInterface = getLoadedPowerInterface(blockAccess, x, y, z, fromBlockFace);
        return powerInterface == null ? 0 : powerInterface.sendFoxPower(amount);
    }

    public static long computeEnergy(long energyValue, long maxEnergyValue, long energyStep) {
        if (energyValue >= maxEnergyValue) return maxEnergyValue;
        if (energyStep == 1) return energyValue;
        return energyValue - (energyValue % energyStep);
    }

    public static String energyToStringShort(long energy) {
        if (energy < 1000) {
            return energy + " mFE";
        } else if (energy < 10000) {
            return DECIMAL_FORMAT_LOW_FE.format((double) energy / 1000D) + " FE";
        } else if (energy < 100000) {
            return DECIMAL_FORMAT_HIGH_FE.format((double) energy / 1000D) + " FE";
        } else {
            return (energy / 1000L) + " FE";
        }
    }

    public static String energyToStringShortNamed(long energy) {
        StringTranslate stringTranslate = StringTranslate.getInstance();
        if (energy < 1000) {
            return energy + " " + stringTranslate.translateKey("fox-energy.milli-name");
        } else if (energy < 10000) {
            return DECIMAL_FORMAT_LOW_FE.format((double) energy / 1000D) +
                    " " + stringTranslate.translateKey("fox-energy.name");
        } else if (energy < 100000) {
            return DECIMAL_FORMAT_HIGH_FE.format((double) energy / 1000D) +
                    " " + stringTranslate.translateKey("fox-energy.name");
        } else {
            return (energy / 1000L) + " " + stringTranslate.translateKey("fox-energy.name");
        }
    }

    public static String energyToStringFull(long energy) {
        return (energy / 1000L) + "." + (energy % 1000L) + " FE";
    }

    public static String energyToStringFullNamed(long energy) {
        return (energy / 1000L) + "." + (energy % 1000L) + " " +
                StringTranslate.getInstance().translateKey("fox-energy.name");
    }

    public static void updateMaxSinkPriorityValue() {
        // The "maxSinkPriorityValue" should stay the same during a world lifecycle.
        FoxPowerUtils.maxSinkPriorityValue = ModLoaderOptions.INSTANCE.maxSinkPriorityValue;
    }
}
