package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.registry.RegisteredEntityItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.src.game.entity.other.EntityItem;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(EntityItem.class)
public class MixinEntityItem implements RegisteredEntityItem {
    @Shadow public ItemStack item;

    @Override
    public RegisteredItemStack getRegisteredItemStack() {
        return ClientMod.toRegisteredItemStack(this.item);
    }

    @Override
    public void setRegisteredItemStack(RegisteredItemStack registeredItemStack) {
        this.item = ClientMod.toItemStack(Objects.requireNonNull(registeredItemStack));
    }
}
