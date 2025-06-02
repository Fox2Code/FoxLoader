/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
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

import blue.endless.jankson.api.document.*;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoaderInit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

public final class ConfigStructure {
    private static final IdentityHashMap<Class<?>, ConfigStructure> cache = new IdentityHashMap<>();
    private static final Pattern newLine = Pattern.compile("\\r?\\n");

    public static ConfigStructure parseFromClass(Class<?> cls, ModContainer modContainer) {
        if (modContainer == null) {
            throw new IllegalArgumentException("modContainer must not be null");
        }
        ConfigStructure configStructure = cache.get(cls);
        if (configStructure != null) {
            return configStructure;
        }
        synchronized (cache) {
            configStructure = cache.get(cls);
            if (configStructure != null) {
                return configStructure;
            }
            HashMap<String, ConfigKey> configKeyHashMap = new HashMap<>();
            IdentityHashMap<Class<?>, ConfigMenu> menuCache = new IdentityHashMap<>();
            configStructure = new ConfigStructure(
                    Collections.unmodifiableMap(configKeyHashMap),
                    parseMenuImpl(cls, modContainer, "", null, null,
                            configKeyHashMap, menuCache), cls, modContainer.getModId());
            cache.put(cls, configStructure);
        }
        return configStructure;
    }

    private static ConfigMenu parseMenuImpl(
            Class<?> cls, ModContainer modContainer,
            String prefix, ConfigKey declaringConfigKey,
            ArrayList<ConfigKey> configKeys,
            HashMap<String, ConfigKey> configKeyHashMap,
            IdentityHashMap<Class<?>, ConfigMenu> menuCache) {
        ConfigMenu configMenu = menuCache.get(cls);
        if (menuCache.containsKey(cls)) {
            ModLoaderInit.getModLoaderLogger().warning(
                    "Config menu loop detected for class: " + cls.getName());
            return configMenu;
        }

        if (declaringConfigKey != null) {
            Objects.requireNonNull(configKeys, "SubMenu need keys");
            configMenu = declaringConfigKey.configMenu;
        } else {
            configKeys = new ArrayList<>();
            configMenu = new ConfigMenu(modContainer.getModName() + " Config",
                    "config." + modContainer.getModId() + "." + prefix + "menu",
                    Collections.unmodifiableList(configKeys));
        }

        if (cls.isAssignableFrom(NoConfigObject.class)) {
            return configMenu;
        }

        menuCache.put(cls, configMenu);
        for (Field field : cls.getFields()) {
            Class<?> fieldType = field.getType();
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            if (configEntry == null) continue;
            if ((field.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE |
                    Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL)) != Modifier.PUBLIC) {
                ModLoaderInit.getModLoaderLogger().warning("Field " +
                        field.getDeclaringClass().getName() + "#" + field.getName() +
                        " has ConfigEntry annotation but is not a public virtual mutable field");
                continue;
            }
            ConfigEntryType type = configEntry.type();
            if (type == ConfigEntryType.AUTO) {
                for (ConfigEntryType configEntryType : ConfigEntryType.values()) {
                    if (configEntryType.isValidField(field)) {
                        type = configEntryType;
                        break;
                    }
                }
            }
            if (!type.isValidField(field)) {
                ModLoaderInit.getModLoaderLogger().warning(
                        "Invalid config field detected for: " +
                                cls.getName() + "#" + field.getName());
                continue;
            }
            final String defaultConfigPath = prefix + field.getName();
            String configPath = configEntry.configPath().replace("${fieldName}", field.getName())
                    .replace("${modId}", modContainer.getModId()).replace("${defaultConfigPath}", defaultConfigPath);
            String configTranslation = configEntry.configTranslation().replace("${fieldName}", field.getName())
                    .replace("${modId}", modContainer.getModId()).replace("${defaultConfigPath}", defaultConfigPath)
                    .replace("${configPath}", configPath);
            ConfigKey configKey;
            Method handler = null;
            String handlerName = configEntry.handlerName();
            if (handlerName != null && !handlerName.isEmpty()) {
                try {
                    handler = cls.getMethod(handlerName);
                } catch (Exception e) {
                    ModLoaderInit.getModLoaderLogger().warning(
                            "Failed to find " + field.getName() +  " handler: " +
                                    cls.getName() + "." + handlerName + "()");
                }
            }
            switch (type) {
                case CONFIG: {
                    ConfigKey.ConfigElement configElement;
                    if (configKeyHashMap.containsKey(configPath)) {
                        ModLoaderInit.getModLoaderLogger().warning(
                                "Field path option duplicate for: " +
                                        cls.getName() + "#" + field.getName());

                        configElement = ConfigKey.ConfigElement.DUPLICATE;
                    } else if (fieldType.isPrimitive() && fieldType != boolean.class) {
                        configElement = ConfigKey.ConfigElement.SLIDER;
                    } else if (fieldType == String.class) {
                        configElement = ConfigKey.ConfigElement.TEXT;
                    } else {
                        configElement = ConfigKey.ConfigElement.BUTTON;
                    }
                    configKey = new ConfigKey(configEntry, ConfigEntryType.CONFIG, configElement,
                            null, declaringConfigKey, configTranslation, configPath, field, handler);
                    if (configElement != ConfigKey.ConfigElement.DUPLICATE) {
                        configKeyHashMap.put(configPath, configKey);
                    }
                    break;
                }
                case SUBMENU: {
                    final String subMenuPrefix = configPath + ".";
                    final ArrayList<ConfigKey> subMenuConfigKeys = new ArrayList<>();
                    ConfigMenu subConfigMenu = new ConfigMenu(modContainer.getModName() + " Config",
                            "config." + modContainer.getModId() + "." + subMenuPrefix + "menu",
                            Collections.unmodifiableList(configKeys));
                    configKey = new ConfigKey(configEntry, ConfigEntryType.SUBMENU, ConfigKey.ConfigElement.BUTTON,
                            subConfigMenu, declaringConfigKey, configTranslation, configPath, field, handler);
                    ConfigMenu subConfigMenuResult =
                            parseMenuImpl(fieldType, modContainer, subMenuPrefix,
                                    configKey, subMenuConfigKeys, configKeyHashMap, menuCache);
                    if (subConfigMenuResult != subConfigMenu) {
                        // This can happen if the menu was already cached, to avoid infinite loops
                        configKey = new ConfigKey(configEntry, ConfigEntryType.SUBMENU, ConfigKey.ConfigElement.BUTTON,
                                subConfigMenuResult, declaringConfigKey, configTranslation, configPath, field, handler);
                    }
                    break;
                }
                case LINK: {
                    configKey = new ConfigKey(configEntry, ConfigEntryType.LINK, ConfigKey.ConfigElement.BUTTON,
                            null, declaringConfigKey, configTranslation, configPath, field, handler);
                    break;
                }
                default: {
                    throw new AssertionError("Missing case statement for " + type.name());
                }
            }
            configKeys.add(configKey);
        }
        menuCache.remove(cls);
        return configMenu;
    }

    public final Map<String, ConfigKey> configKeyMap;
    public final ConfigMenu rootConfigMenu;
    public final Class<?> cls;
    public final String modId;

    private ConfigStructure(Map<String, ConfigKey> configKeyMap, ConfigMenu rootConfigMenu,
                            Class<?> cls, String modId) {
        this.configKeyMap = configKeyMap;
        this.rootConfigMenu = rootConfigMenu;
        this.cls = cls;
        this.modId = modId;
    }

    public void loadJsonConfig(ObjectElement jsonObject, Object config) {
        this.cls.cast(config); // Verify input type
        for (ConfigKey configKey : this.configKeyMap.values()) {
            ValueElement element = getElementJsonObject(jsonObject, configKey.path);
            if (element != null) {
                setElementConfigKey(config, configKey, element);
            }
        }
    }

    public ObjectElement saveJsonConfig(Object config) {
        this.cls.cast(config); // Verify input type
        ObjectElement jsonObject = new ObjectElement();
        for (ConfigKey configKey : this.configKeyMap.values()) {
            ValueElement element = getElementConfigKey(config, configKey);
            if (element != null) {
                setElementJsonObject(jsonObject, configKey.path, configKey.configEntry.configComment(), element);
            }
        }
        return jsonObject;
    }

    private static ValueElement getElementConfigKey(Object instance, ConfigKey configKey) {
        Object configValue = Internal.getInstanceConfigKeyImpl(instance, configKey);
        if (configValue == null) {
            return null;
        }
        if (configValue instanceof Enum) {
            configValue = ((Enum<?>) configValue).name();
        }
        if (configValue instanceof String) {
            return PrimitiveElement.of((String) configValue);
        }
        if (configValue instanceof Boolean) {
            return PrimitiveElement.of((Boolean) configValue);
        }
        if (configValue instanceof Number) {
            if (configValue instanceof Float || configValue instanceof Double) {
                return PrimitiveElement.of(((Number) configValue).doubleValue());
            }
            return PrimitiveElement.of(((Number) configValue).longValue());
        }
        throw new AssertionError("Config allow type " + configValue.getClass().getName());
    }

    private static void setElementConfigKey(Object instance, ConfigKey configKey, ValueElement jsonElement) {
        if (!(jsonElement instanceof PrimitiveElement)) {
            return;
        }
        PrimitiveElement jsonPrimitive = (PrimitiveElement) jsonElement;
        Class<?> type = configKey.field.getType();
        Object configValue = null;

        if (type == boolean.class) {
            configValue = jsonPrimitive.asBoolean().orElse(null);
        } else if (type != char.class && type.isPrimitive()) {
            configValue = Internal.correctNumber(jsonPrimitive, type);
        } else if (type == String.class) {
            configValue = jsonPrimitive.asString().orElse(null);
        }
        if (configValue != null) {
            Internal.setInstanceConfigKeyImpl(instance, configKey, configValue);
        }
    }

    private static ValueElement getElementJsonObject(ObjectElement jsonObject, String path) {
        ObjectElement directJsonObject = jsonObject;
        int index = 0;
        int tmpIndex;
        while ((tmpIndex = path.indexOf('.', index)) != -1) {
            String subKey = path.substring(index, tmpIndex);
            if (directJsonObject == null || !directJsonObject.containsKey(subKey)) {
                return null;
            }
            directJsonObject = directJsonObject.getObject(subKey);
            index = tmpIndex + 1;
        }
        return directJsonObject == null ? null : directJsonObject.get(path.substring(index));
    }

    private static void setElementJsonObject(
            ObjectElement jsonObject, String path, String description, ValueElement jsonElement) {
        ObjectElement directJsonObject = jsonObject;
        int index = 0;
        int tmpIndex;
        int depth = 1;
        while ((tmpIndex = path.indexOf('.', index)) != -1) {
            String subKey = path.substring(index, tmpIndex);
            if (directJsonObject.containsKey(subKey)) {
                directJsonObject = directJsonObject.getObject(subKey);
            } else {
                ObjectElement oldJsonObject = directJsonObject;
                directJsonObject = new ObjectElement();
                oldJsonObject.put(subKey, directJsonObject);
            }
            index = tmpIndex + 1;
            depth++;
        }
        String key = path.substring(index);
        directJsonObject.put(key, jsonElement);
        Optional<KeyValuePairElement> keyValuePairElementOptional = directJsonObject.getKeyValuePair(key);
        if (keyValuePairElementOptional.isPresent()) {
            KeyValuePairElement keyValuePairElement = keyValuePairElementOptional.get();
            keyValuePairElement.getPrologue().clear();
            if (description != null && !description.isEmpty()) {
                for (String descriptionLine : newLine.split(description)) {
                    keyValuePairElement.getPrologue().add(
                            new CommentElement(" " + descriptionLine, CommentType.LINE_END));
                }
            }
        }
    }

    public static class Internal {
        public static Object getInstanceConfigKeyImpl(Object instance, ConfigKey configKey) {
            if (instance == null) return null;
            if (configKey.parent != null) {
                instance = getInstanceConfigKeyImpl(instance, configKey);
            }
            if (instance == null) return null;
            try {
                return configKey.field.get(instance);
            } catch (IllegalAccessException e) {
                throw new AssertionError("All config fields should be accessible", e);
            }
        }

        public static void setInstanceConfigKeyImpl(Object instance, ConfigKey configKey, Object value) {
            if (instance == null) return;
            if (configKey.parent != null) {
                instance = getInstanceConfigKeyImpl(instance, configKey.parent);
            }
            if (instance == null) return;
            try {
                configKey.field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new AssertionError("All config fields should be accessible", e);
            }
        }

        public static Number correctNumber(PrimitiveElement primitiveElement, Class<?> type) {
            Number number = null;
            if (type == float.class || type == double.class) {
                OptionalDouble optionalDouble = primitiveElement.asDouble();
                if (optionalDouble.isPresent()) {
                    number = optionalDouble.getAsDouble();
                }
            } else if (type == int.class) {
                OptionalInt optionalInt = primitiveElement.asInt();
                if (optionalInt.isPresent()) {
                    number = optionalInt.getAsInt();
                }
            }
            if (number == null) {
                OptionalLong optionalLong = primitiveElement.asLong();
                if (optionalLong.isPresent()) {
                    number = optionalLong.getAsLong();
                }
            }
            if (number == null) {
                return null;
            }
            return correctNumber(number, type);
        }

        public static Number correctNumber(Number number, Class<?> type) {
            if (type == byte.class) {
                return number instanceof Byte ? number : number.byteValue();
            }
            if (type == short.class) {
                return number instanceof Short ? number : number.shortValue();
            }
            if (type == int.class) {
                return number instanceof Integer ? number : number.intValue();
            }
            if (type == long.class) {
                return number instanceof Long ? number : number.longValue();
            }
            if (type == float.class) {
                return number instanceof Float ? number : number.floatValue();
            }
            if (type == double.class) {
                return number instanceof Double ? number : number.doubleValue();
            }
            throw new AssertionError("Unsupported number type: " + type.getName());
        }
    }
}
