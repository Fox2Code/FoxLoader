package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.registry.RegisteredEntity;
import com.fox2code.foxloader.registry.RegisteredWorld;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public class MixinEntity implements RegisteredEntity {
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public World worldObj;

    @Override
    public RegisteredWorld getCurrentRegisteredWorld() {
        return (RegisteredWorld) this.worldObj;
    }

    @Override
    public double getRegisteredX() {
        return this.posX;
    }

    @Override
    public double getRegisteredY() {
        return this.posY;
    }

    @Override
    public double getRegisteredZ() {
        return this.posZ;
    }
}
