package com.fox2code.foxloader.server.mixins;

import net.minecraft.src.game.nbt.NBTTagIntArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@Mixin(NBTTagIntArray.class)
public class MixinNBTTagIntArray {
    @Shadow public int[] intArray;

    /**
     * Fix NBTTagIntArray saving as NBTTagByteArray
     *
     * @author Fox2Code
     * @reason Just a hotfix
     */
    @Overwrite
    public byte getType() {
        return 11;
    }

    /**
     * Fix NBTTagIntArray not saving/loading anything
     *
     * @author Fox2Code
     * @reason Just a hotfix
     */
    @Overwrite
    void writeTagContents(DataOutput var1) throws IOException {
        var1.writeInt(this.intArray.length);
        for (int i : this.intArray) {
            var1.writeInt(i);
        }
    }

    /**
     * Fix NBTTagIntArray not saving/loading anything
     *
     * @author Fox2Code
     * @reason Just a hotfix
     */
    @Overwrite
    void readTagContents(DataInput var1) throws IOException {
        final int var2 = var1.readInt();
        this.intArray = new int[var2];
        for (int i = 0; i < var2; i++) {
            this.intArray[i] = var1.readInt();
        }
    }
}
