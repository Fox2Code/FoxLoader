package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredEntity;
import com.fox2code.foxloader.registry.RegisteredEntityLiving;
import net.minecraft.src.game.effect.Effect;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.EntityLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityLiving.class)
public abstract class MixinEntityLiving implements RegisteredEntityLiving {
    @Shadow private Entity currentTarget;
    @Shadow public int[] effecttimers;
    @Shadow public byte[] effectlevel;

    @Shadow public abstract boolean hasEffect(Effect effect);

    @Override
    public RegisteredEntity getRegisteredTarget() {
        return (RegisteredEntity) this.currentTarget;
    }

    @Override
    public void setRegisteredTarget(RegisteredEntity registeredTarget) {
        this.currentTarget = (Entity) registeredTarget;
    }

    @Override
    public boolean getRegisteredHasEffect(int effectId) {
        return this.hasEffect(Effect.effectlist[effectId]);
    }

    @Override
    public int getRegisteredEffectTimer(int effectId) {
        return this.effecttimers[effectId];
    }

    @Override
    public int getRegisteredEffectLevel(int effectId) {
        return this.effecttimers[effectId] <= 0 ? 0 : this.effectlevel[effectId];
    }

    @Override
    public void giveEffectRegistered(int effectId, int effectLevel, int effectDuration) {
        Effect.applyEffect((EntityLiving) (Object) this, effectId, effectLevel, effectDuration);
    }
}
