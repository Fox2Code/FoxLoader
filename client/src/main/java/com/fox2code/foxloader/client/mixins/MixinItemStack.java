package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.NetworkItemStack;
import com.fox2code.foxloader.registry.GameRegistryClient;
import com.fox2code.foxloader.registry.RegisteredItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements RegisteredItemStack, NetworkItemStack {
    @Shadow public int itemID;
    @Shadow public int stackSize;
    @Shadow public int itemDamage;
    @Unique private int networkId;

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

    @Inject(method = "splitStack", at = @At("RETURN"))
    public void onSplitStack(int stacksize, CallbackInfoReturnable<ItemStack> cir) {
        ((NetworkItemStack) (Object) cir.getReturnValue()).setRemoteNetworkId(this.networkId);
    }

    @Inject(method = "copy", at = @At("RETURN"))
    public void onCopy(CallbackInfoReturnable<ItemStack> cir) {
        ((NetworkItemStack) (Object) cir.getReturnValue()).setRemoteNetworkId(this.networkId);
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
    public int getRemoteItemId() {
        int networkId = this.networkId;
        if (networkId != 0)
            return networkId;
        return GameRegistryClient.itemIdMappingOut[this.itemID];
    }

    @Override
    public void setRemoteNetworkId(int networkId) {
        this.networkId = networkId;
    }

    @Override
    public void verifyRegisteredItemStack() {}
}
