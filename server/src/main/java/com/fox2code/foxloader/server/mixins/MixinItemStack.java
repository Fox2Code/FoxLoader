package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements RegisteredItemStack {
    @Shadow public int itemID;
    @Shadow public int stackSize;
    @Shadow public int itemDamage;
    @Shadow public NBTTagCompound nbtTagCompound;

    @Shadow public abstract Item getItem();
    @Shadow public abstract String getDisplayName();
    @Shadow public abstract void setItemName(String par1Str);
    @Shadow public abstract boolean hasDisplayName();

    @Inject(method = "<init>(II)V", at = @At("RETURN"))
    public void onNewItemStack(int id, int count, CallbackInfo ci) {
        this.verifyRegisteredItemStack();
    }

    @Inject(method = "<init>(III)V", at = @At("RETURN"))
    public void onNewItemStack(int id, int count, int damage, CallbackInfo ci) {
        this.verifyRegisteredItemStack();
    }

    @Inject(method = "<init>(IIILnet/minecraft/src/game/nbt/NBTTagCompound;)V", at = @At("RETURN"))
    public void onNewItemStack(int id, int count, int damage, NBTTagCompound tagCompound, CallbackInfo ci) {
        this.verifyRegisteredItemStack();
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    public void onReadFromNBT(NBTTagCompound nbtTagCompound, CallbackInfo ci) {
        this.verifyRegisteredItemStack();
    }

    @Override
    public RegisteredItem getRegisteredItem() {
        return (RegisteredItem) this.getItem();
    }

    @Override
    public int getRegisteredStackSize() {
        return this.stackSize;
    }

    @Override
    public void setRegisteredStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    @Override
    public int getRegisteredDamage() {
        return this.itemDamage;
    }

    @Override
    public void setRegisteredDamage(int damage) {
        this.itemDamage = damage;
    }

    @Override
    public String getRegisteredDisplayName() {
        return this.hasDisplayName() ? this.getDisplayName() : null;
    }

    @Override
    public void setRegisteredDisplayName(String displayName) {
        this.setItemName(displayName);
    }

    @Override
    public int getRegisteredDynamicTextureId() {
        NBTTagCompound nbtTagCompound = this.nbtTagCompound;
        return nbtTagCompound == null || // Avoid NPEs here
                !nbtTagCompound.hasKey("DynamicTextureId") ? -1 :
                nbtTagCompound.getByte("DynamicTextureId");
    }

    @Override
    public void setRegisteredDynamicTextureId(int dynamicTextureSlot) {
        NBTTagCompound nbtTagCompound = this.nbtTagCompound;
        if (dynamicTextureSlot == -1) {
            if (nbtTagCompound != null)
                nbtTagCompound.removeTag("DynamicTextureId");
            return;
        }
        if (nbtTagCompound == null) {
            this.nbtTagCompound = nbtTagCompound = new NBTTagCompound();
        }
        nbtTagCompound.setByte("DynamicTextureId", (byte) dynamicTextureSlot);
    }

    @Override
    public void verifyRegisteredItemStack() {}
}
