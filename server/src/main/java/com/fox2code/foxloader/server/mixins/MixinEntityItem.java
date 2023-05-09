package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ServerMod;
import com.fox2code.foxloader.registry.RegisteredEntityItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.src.game.entity.other.EntityItem;
import net.minecraft.src.game.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(EntityItem.class)
public class MixinEntityItem implements RegisteredEntityItem {
    @Shadow
    public ItemStack item;

    @Override
    public RegisteredItemStack getRegisteredItemStack() {
        return ServerMod.toRegisteredItemStack(this.item);
    }

    @Override
    public void setRegisteredItemStack(RegisteredItemStack registeredItemStack) {
        this.item = ServerMod.toItemStack(Objects.requireNonNull(registeredItemStack));
    }
}
