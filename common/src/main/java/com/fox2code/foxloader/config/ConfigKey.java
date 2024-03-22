package com.fox2code.foxloader.config;

import com.fox2code.foxloader.loader.ModLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class ConfigKey {
    public final ConfigEntry configEntry;
    public final ConfigElement configElement;
    public final ConfigMenu configMenu;
    public final ConfigKey parent;
    public final String translation;
    public final String path;
    public final Field field;
    public final Method handler;

    ConfigKey(ConfigEntry configEntry, ConfigElement configElement,
              ConfigMenu configMenu, ConfigKey parent, String translation, String path, Field field, Method handler) {
        this.configEntry = configEntry;
        this.configElement = configElement;
        this.configMenu = configMenu;
        this.parent = parent;
        this.translation = translation;
        this.path = path;
        this.field = field;
        this.handler = handler;
    }

    public Object getField(Object instance) {
        try {
            return this.field.get(instance);
        } catch (IllegalAccessException e) {
            throw new AssertionError("All config fields should be accessible", e);
        }
    }

    public void setField(Object instance, Object value) {
        try {
            this.field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError("All config fields should be accessible", e);
        }
    }

    public void callHandler(Object instance) {
        if (this.handler != null) {
            try {
                this.handler.invoke(instance);
            } catch (IllegalAccessException e) {
                throw new AssertionError("All config methods should be accessible", e);
            } catch (Error | InvocationTargetException e) {
                ModLoader.getModLoaderLogger().log(Level.WARNING, "Mod handler call failed", e);
            }
        }
    }

    public enum ConfigElement {
        DUPLICATE, BUTTON, SLIDER, TEXT
    }
}
