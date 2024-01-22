package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.lua.LuaObjectHolder;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredEntity;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.nbt.NBTTagCompound;
import org.luaj.vm2.LuaValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer implements LuaObjectHolder {
    @Inject(method = "useCurrentItemOnEntity", at = @At("HEAD"), cancellable = true)
    public void onUseCurrentItemOnEntity(Entity var1, CallbackInfo ci) {
        if (!(this instanceof NetworkPlayer)) return;
        NetworkPlayer networkPlayer = (NetworkPlayer) this;
        if (ModLoader.Internal.notifyPlayerUseItemOnEntity(networkPlayer,
                networkPlayer.getRegisteredHeldItem(), (RegisteredEntity) var1)) {
            ci.cancel();
        }
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"), cancellable = true)
    public void onAttackTargetEntityWithCurrentItem(Entity var1, CallbackInfo ci) {
        if (!(this instanceof NetworkPlayer)) return;
        NetworkPlayer networkPlayer = (NetworkPlayer) this;
        if (ModLoader.Internal.notifyPlayerAttackEntity(networkPlayer,
                networkPlayer.getRegisteredHeldItem(), (RegisteredEntity) var1)) {
            ci.cancel();
        }
    }

    @Redirect(method = "readEntityFromNBT", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/src/game/nbt/NBTTagCompound;getLong(Ljava/lang/String;)J"))
    public long hotfix_getLongRedirect(NBTTagCompound instance, String var1) {
        try {
            return instance.getLong(var1);
        } catch (ClassCastException e) {
            try {
                return instance.getInteger(var1);
            } catch (ClassCastException ignored) {}
            throw e;
        }
    }

    // Lua object holder
    @Unique
    private WeakReference<LuaValue> foxLoader$LuaObject;

    @Override
    public WeakReference<LuaValue> foxLoader$getLuaObject() {
        return this.foxLoader$LuaObject;
    }

    @Override
    public void foxLoader$setLuaObject(WeakReference<LuaValue> foxLoader$LuaObject) {
        this.foxLoader$LuaObject = foxLoader$LuaObject;
    }
}
