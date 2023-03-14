package com.fox2code.foxloader.loader.mixin;

import com.fox2code.foxloader.launcher.FoxLauncher;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;

public class MixinPropertyService implements IGlobalPropertyService {
    static class Key implements IPropertyKey {

        private final String key;

        Key(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(IPropertyKey key) {
        return (T) FoxLauncher.mixinProperties.get(key.toString());
    }

    @Override
    public final void setProperty(IPropertyKey key, Object value) {
        FoxLauncher.mixinProperties.put(key.toString(), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T) FoxLauncher.mixinProperties.getOrDefault(key.toString(), defaultValue);
    }

    @Override
    public final String getPropertyString(IPropertyKey key, String defaultValue) {
        return FoxLauncher.mixinProperties.getOrDefault(key.toString(), defaultValue).toString();
    }
}
