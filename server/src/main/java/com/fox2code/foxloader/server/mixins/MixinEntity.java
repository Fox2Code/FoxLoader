package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.registry.RegisteredEntity;
import com.fox2code.foxloader.registry.RegisteredWorld;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class MixinEntity implements RegisteredEntity {
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public World worldObj;
    @Shadow public Entity ridingEntity;
    @Shadow public Entity riddenByEntity;

    @Shadow public abstract void setPosition(double var1, double var3, double var5);
    @Shadow protected abstract void kill();

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

    @Override
    public void teleportRegistered(double x, double y, double z) {
        this.setPosition(x, y, z);
    }

    @Override
    public void killRegistered() {
        this.kill();
    }

    @Override
    public RegisteredEntity getRegisteredRidding() {
        return (RegisteredEntity) this.ridingEntity;
    }

    @Override
    public RegisteredEntity getRegisteredRiddenBy() {
        return (RegisteredEntity) this.riddenByEntity;
    }
}
