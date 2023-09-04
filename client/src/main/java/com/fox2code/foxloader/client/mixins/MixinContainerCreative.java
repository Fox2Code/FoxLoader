package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.CreativeItems;
import com.fox2code.foxloader.client.gui.ContainerWrapped;
import net.minecraft.src.client.gui.Container;
import net.minecraft.src.client.gui.ContainerCreative;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ContainerCreative.class)
public abstract class MixinContainerCreative implements ContainerWrapped {
    @Shadow public List<ItemStack> itemList;

    @Shadow public abstract void addToSlot(ItemStack itemstack);

    @Shadow private EntityPlayer player;

    @Inject(method = "addItems", at = @At("HEAD"), cancellable = true)
    public void onAddItems(EntityPlayer player, CallbackInfo ci) {
        List<ItemStack> creativeItemStacks =
                CreativeItems.Internal.computedModdedStacks;
        if (creativeItemStacks != null) {
            this.itemList = new ArrayList<>();
            for (ItemStack item: creativeItemStacks) {
                addToSlot(item);
            }
            ci.cancel();
        }
    }

    @Inject(method = "addItems", at = @At("TAIL"))
    public void onItemsAdded(EntityPlayer player, CallbackInfo ci) {
        CreativeItems.Internal.markLoadFinished(this.itemList);
    }

    @Override
    public Container getParentContainer() {
        return this.player.playerContainer;
    }
}
