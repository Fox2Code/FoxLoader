package com.fox2code.foxloader.registry;

public interface RegisteredEntityLiving extends RegisteredEntity {
    default RegisteredEntity getRegisteredTarget() { throw new RuntimeException(); }

    default void setRegisteredTarget(RegisteredEntity registeredTarget) { throw new RuntimeException(); }

    default boolean getRegisteredHasEffect(int effectId) { throw new RuntimeException(); }

    default int getRegisteredEffectTimer(int effectId) { throw new RuntimeException(); }

    default int getRegisteredEffectLevel(int effectId) { throw new RuntimeException(); }

    default void giveEffectRegistered(int effectId, int effectLevel, int effectDuration) { throw new RuntimeException(); }
}
