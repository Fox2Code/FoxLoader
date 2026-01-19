/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.config;

import com.fox2code.foxloader.loader.ModLoaderInit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public final class ConfigKey {
    public final ConfigEntry configEntry;
    public final ConfigEntryType configEntryType;
    public final ConfigElement configElement;
    public final ConfigMenu configMenu;
    public final ConfigKey parent;
    public final String translation;
    public final String path;
    public final Field field;
    public final Method handler;

    ConfigKey(ConfigEntry configEntry, ConfigEntryType configEntryType, ConfigElement configElement,
              ConfigMenu configMenu, ConfigKey parent, String translation, String path, Field field, Method handler) {
        this.configEntry = configEntry;
        this.configEntryType = configEntryType;
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
                ModLoaderInit.getModLoaderLogger().log(Level.WARNING, "Mod handler call failed", e);
            }
        }
    }

    public enum ConfigElement {
        DUPLICATE, BUTTON, SLIDER, TEXT
    }
}
