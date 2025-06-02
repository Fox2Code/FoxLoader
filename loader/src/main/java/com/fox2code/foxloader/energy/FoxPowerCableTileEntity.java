package com.fox2code.foxloader.energy;

import com.mojang.nbt.CompoundTag;
import net.minecraft.common.block.tileentity.TileEntity;
import net.minecraft.common.util.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class FoxPowerCableTileEntity extends TileEntity {
    private final FoxPowerCableBlock cableBlock;
    public final FoxPowerInterface powerInterface;
    private final int[] sinkPriorities;
    private long storedFoxPower;
    private int sinkPriority;
    private int sinkSource;

    public FoxPowerCableTileEntity(FoxPowerCableBlock cableBlock) {
        this.cableBlock = cableBlock;
        this.powerInterface = this.makeFoxPowerInterface();
        this.sinkPriorities = new int[6];
        this.storedFoxPower = 0;
        this.sinkPriority = 1;
        this.sinkSource = -1;
    }

    protected FoxPowerInterface makeFoxPowerInterface() {
        return new FoxPowerInterfaceCable(this);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        this.storedFoxPower = tag.getLong("storedFoxPower");
        // Compute new state from saved sinkPriorities
        int[] ints = tag.getIntArray("sinkPriorities");
        if (ints == null || ints.length != 6) {
            Arrays.fill(this.sinkPriorities, 0);
        } else {
            System.arraycopy(ints, 0, this.sinkPriorities, 0, 6);
        }
        int cableSinkPriority = 1;
        int cableSinkFaceSource = -1;
        for (int i = 0; i < 6; i++) {
            int faceSinkPriority = this.sinkPriorities[i];
            // "maxSinkPriorityValue" might have been changed.
            if (faceSinkPriority > FoxPowerUtils.maxSinkPriorityValue) {
                faceSinkPriority = FoxPowerUtils.maxSinkPriorityValue;
                this.sinkPriorities[i] = faceSinkPriority;
            }
            faceSinkPriority--;
            if (faceSinkPriority >= cableSinkPriority) {
                cableSinkPriority = faceSinkPriority;
                cableSinkFaceSource = i;
            }
        }
        this.sinkPriority = cableSinkPriority;
        this.sinkSource = cableSinkFaceSource;
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.setLong("storedFoxPower", this.storedFoxPower);
        tag.setIntArray("sinkPriorities", this.sinkPriorities);
    }

    public void updateSinkPriorities() {
        int cableSinkPriority = 1;
        int cableSinkFaceSource = -1;
        final int oldCableSinkPriority = this.sinkPriority;
        final int oldCableSinkFaceSource = this.sinkSource;
        final int oldCableSinkFaceSourcePriority = // For fast un-propagation.
                oldCableSinkFaceSource == -1 ? 0 : this.sinkPriorities[oldCableSinkFaceSource];
        for (Direction.EnumDirection blockFace : Direction.EnumDirection.VALID_DIRECTIONS) {
            FoxPowerInterface powerInterface = FoxPowerUtils.getPowerInterface(this.worldObj,
                    this.xCoord + blockFace.offsX, this.yCoord + blockFace.offsY,
                    this.zCoord + blockFace.offsZ, blockFace.ordinal() ^ 1);
            int faceSinkPriority = powerInterface == null ? 0 :
                    Math.min(FoxPowerUtils.maxSinkPriorityValue, powerInterface.getCableSinkPriority());
            this.sinkPriorities[blockFace.ordinal()] = faceSinkPriority;
            faceSinkPriority -= 1;
            if (faceSinkPriority >= cableSinkPriority) {
                cableSinkPriority = faceSinkPriority;
                cableSinkFaceSource = blockFace.ordinal();
            }
        }
        // Skip full power cable calculations on client only worlds
        if (!this.worldObj.isRemote) {
            if (this.updateSinkPriorityValues(oldCableSinkFaceSource,
                    oldCableSinkPriority, oldCableSinkFaceSourcePriority,
                    cableSinkPriority, cableSinkFaceSource)) {
                FoxPowerUtils.notifyNeighboringPowerBlocks(
                        this.worldObj, this.xCoord, this.yCoord, this.zCoord);
            }
        }
    }

    public boolean updateSinkPriorityForFace(Direction.EnumDirection blockFace) {
        FoxPowerInterface powerInterface = FoxPowerUtils.getPowerInterface(this.worldObj,
                this.xCoord + blockFace.offsX, this.yCoord + blockFace.offsY,
                this.zCoord + blockFace.offsZ, blockFace.ordinal() ^ 1);
        this.sinkPriorities[blockFace.ordinal()] = powerInterface == null ? 0 :
                Math.min(FoxPowerUtils.maxSinkPriorityValue, powerInterface.getCableSinkPriority());
        if (worldObj.isRemote) {
            // Skip any expensive calculations on client only worlds
            return false;
        }
        int cableSinkPriority = 1;
        int cableSinkFaceSource = -1;
        final int oldCableSinkPriority = this.sinkPriority;
        final int oldCableSinkFaceSource = this.sinkSource;
        final int oldCableSinkFaceSourcePriority = // For fast un-propagation.
                oldCableSinkFaceSource == -1 ? 0 : this.sinkPriorities[oldCableSinkFaceSource];
        for (int i = 0; i < 6; i++) {
            int faceSinkPriority = this.sinkPriorities[i] - 1;
            if (faceSinkPriority >= cableSinkPriority) {
                cableSinkPriority = faceSinkPriority;
                cableSinkFaceSource = i;
            }
        }
        return this.updateSinkPriorityValues(oldCableSinkFaceSource,
                oldCableSinkPriority, oldCableSinkFaceSourcePriority,
                cableSinkPriority, cableSinkFaceSource);
    }

    protected final boolean updateSinkPriorityValues(
            int oldCableSinkFaceSource, int oldCableSinkPriority, int oldCableSinkFaceSourcePriority,
            int cableSinkPriority, int cableSinkFaceSource) {
        if (oldCableSinkFaceSource != -1 && cableSinkPriority < oldCableSinkPriority &&
                this.sinkPriorities[oldCableSinkFaceSource] < oldCableSinkFaceSourcePriority) {
            // Allow power sink to un-propagate faster, this is an attempt to get "O(n)" un-propagation.
            cableSinkPriority = Math.max(this.sinkPriorities[oldCableSinkFaceSource] - 1, 1);
        }
        this.sinkSource = cableSinkFaceSource;
        this.sinkPriority = cableSinkPriority;
        return cableSinkPriority != oldCableSinkPriority;
    }

    @Override
    public void updateEntity() {
        int powerDumpingPriorityTarget = this.sinkSource;
        long maxSendPower = Math.min(this.cableBlock.getCableThroughput(), this.storedFoxPower);
        long totalSentPower = 0L;
        if (powerDumpingPriorityTarget == -1) {
            return; // Skip if no power sink is found.
        }
        Direction.EnumDirection direction =
                Direction.EnumDirection.VALID_DIRECTIONS[powerDumpingPriorityTarget];
        FoxPowerInterface foxPowerInterface = FoxPowerUtils.getLoadedPowerInterface(this.worldObj,
                this.xCoord + direction.offsX, this.yCoord + direction.offsY, this.zCoord + direction.offsZ,
                powerDumpingPriorityTarget ^ 1);
        if (foxPowerInterface != null) {
            totalSentPower += foxPowerInterface.sendFoxPower(maxSendPower - totalSentPower);
        }
        if (totalSentPower == maxSendPower) {
            this.storedFoxPower -= totalSentPower;
            return;
        }
        int minSinkPriorityForTransfer = this.sinkPriority;
        for (int i = 0; i < 6; i++) {
            if (i != powerDumpingPriorityTarget && this.sinkPriorities[i] > 0) {
                direction = Direction.EnumDirection.VALID_DIRECTIONS[powerDumpingPriorityTarget];
                foxPowerInterface = FoxPowerUtils.getLoadedPowerInterface(this.worldObj,
                        this.xCoord + direction.offsX, this.yCoord + direction.offsY, this.zCoord + direction.offsZ,
                        i ^ 1);
                if (foxPowerInterface != null) {
                    if (foxPowerInterface.getFoxPowerType() == FoxPowerType.TRANSMITTER &&
                            this.sinkPriorities[i] < minSinkPriorityForTransfer) {
                        continue;
                    }
                    totalSentPower += foxPowerInterface.sendFoxPower(maxSendPower - totalSentPower);
                    if (totalSentPower == maxSendPower) {
                        break;
                    }
                }
            }
        }
        this.storedFoxPower -= totalSentPower;
    }

    public static class FoxPowerInterfaceCable extends FoxPowerInterfaceSimple {
        private final FoxPowerCableTileEntity foxPowerCableTileEntity;

        public FoxPowerInterfaceCable(FoxPowerCableTileEntity foxPowerCableTileEntity) {
            this.foxPowerCableTileEntity = foxPowerCableTileEntity;
        }

        @Override
        public long getMaxFoxPowerStorage() {
            return this.foxPowerCableTileEntity.cableBlock.getCableStorage();
        }

        @Override
        public long getStoredFoxPower() {
            return this.foxPowerCableTileEntity.storedFoxPower;
        }

        @Override
        public long getFoxPowerStorageMaxInput() {
            return this.foxPowerCableTileEntity.cableBlock.getCableThroughput();
        }

        @Override
        public long getFoxPowerStorageMaxOutput() {
            return this.foxPowerCableTileEntity.cableBlock.getCableThroughput();
        }

        @Override
        public void setStoredFoxPower(long foxPower) {
            this.foxPowerCableTileEntity.storedFoxPower = foxPower;
        }

        @Override
        public @NotNull FoxPowerType getFoxPowerType() {
            return FoxPowerType.TRANSMITTER;
        }

        @Override
        public int getCableSinkPriority() {
            // Don't ask for more energy at 90%~ capacity.
            if (((this.getMaxFoxPowerStorage() / 10) * 9) <= this.getStoredFoxPower()) {
                return 0;
            }
            return this.foxPowerCableTileEntity.sinkPriority;
        }

        @Override
        public void emitFoxPowerSinkUpdated() {
            TileEntity tileEntity = this.foxPowerCableTileEntity;
            FoxPowerUtils.notifyNeighboringPowerBlocks(tileEntity.worldObj,
                    tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
        }
    }
}
