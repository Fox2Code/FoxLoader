package com.fox2code.foxloader.config;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigStructure {
    private static final IdentityHashMap<Class<?>, ConfigStructure> cache = new IdentityHashMap<>();

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
                    parseMenuImpl(cls, modContainer, "prefix", null, null,
                            configKeyHashMap, menuCache), cls, modContainer.id);
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
            ModLoader.getModLoaderLogger().warning(
                    "Config menu loop detected for class: " + cls.getName());
            return configMenu;
        }

        if (declaringConfigKey != null) {
            Objects.requireNonNull(configKeys, "SubMenu need keys");
            configMenu = declaringConfigKey.configMenu;
        } else {
            configKeys = new ArrayList<>();
            configMenu = new ConfigMenu(modContainer.name + " Config",
                    "config." + modContainer.id + "." + prefix + "menu",
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
                ModLoader.getModLoaderLogger().warning("Field " +
                        field.getDeclaringClass().getName() + "#" + field.getName() +
                        " has ConfigEntry annotation but is not a public virtual mutable field");
                continue;
            }
            ConfigEntry.ConfigEntryType type = configEntry.type();
            if (!type.isValidField(field)) {
                ModLoader.getModLoaderLogger().warning(
                        "Invalid config field detected for: " +
                                cls.getName() + "#" + field.getName());
                continue;
            }
            final String defaultConfigPath = prefix + field.getName();
            String configPath = configEntry.configPath().replace("${fieldName}", field.getName())
                    .replace("${modId}", modContainer.id).replace("${defaultConfigPath}", defaultConfigPath);
            String configTranslation = configEntry.configPath().replace("${fieldName}", field.getName())
                    .replace("${modId}", modContainer.id).replace("${defaultConfigPath}", defaultConfigPath)
                    .replace("${configPath}", configPath);
            ConfigKey configKey;
            Method handler = null;
            String handlerName = configEntry.handlerName();
            if (handlerName != null && !handlerName.isEmpty()) {
                try {
                    handler = cls.getMethod(handlerName);
                } catch (Exception e) {
                    ModLoader.getModLoaderLogger().warning(
                            "Failed to find " + field.getName() +  " handler: " +
                                    cls.getName() + "." + handlerName + "()");
                }
            }
            switch (type) {
                default: {
                    throw new AssertionError("Missing case statement for " + type.name());
                }
                case CONFIG: {
                    ConfigKey.ConfigElement configElement = ConfigKey.ConfigElement.BUTTON;
                    if (configKeyHashMap.containsKey(configPath)) {
                        ModLoader.getModLoaderLogger().warning(
                                "Field path option duplicate for: " +
                                        cls.getName() + "#" + field.getName());

                        configElement = ConfigKey.ConfigElement.DUPLICATE;
                    } else if (cls.isPrimitive() && cls != boolean.class) {
                        configElement = ConfigKey.ConfigElement.SLIDER;
                    } else if (cls == String.class) {
                        configElement = ConfigKey.ConfigElement.TEXT;
                    }
                    configKey = new ConfigKey(configEntry, configElement,
                            null, declaringConfigKey, configTranslation, configPath, field, handler);
                    if (configElement != ConfigKey.ConfigElement.DUPLICATE) {
                        configKeyHashMap.put(configPath, configKey);
                    }
                    break;
                }
                case SUBMENU: {
                    final String subMenuPrefix = configPath + ".";
                    final ArrayList<ConfigKey> subMenuConfigKeys = new ArrayList<>();
                    ConfigMenu subConfigMenu = new ConfigMenu(modContainer.name + " Config",
                            "config." + modContainer.id + "." + subMenuPrefix + "menu",
                            Collections.unmodifiableList(configKeys));
                    configKey = new ConfigKey(configEntry, ConfigKey.ConfigElement.BUTTON,
                            subConfigMenu, declaringConfigKey, configTranslation, configPath, field, handler);
                    ConfigMenu subConfigMenuResult =
                            parseMenuImpl(fieldType, modContainer, subMenuPrefix,
                                    configKey, subMenuConfigKeys, configKeyHashMap, menuCache);
                    if (subConfigMenuResult != subConfigMenu) {
                        // This can happen if the menu was already cached, to avoid infinite loops
                        configKey = new ConfigKey(configEntry, ConfigKey.ConfigElement.BUTTON,
                                subConfigMenuResult, declaringConfigKey, configTranslation, configPath, field, handler);
                    }
                    break;
                }
                case LINK: {
                    configKey = new ConfigKey(configEntry, ConfigKey.ConfigElement.BUTTON,
                            null, declaringConfigKey, configTranslation, configPath, field, handler);
                    break;
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

    public void loadJsonConfig(JsonObject jsonObject, Object config) {
        this.cls.cast(config); // Verify input type
        for (ConfigKey configKey : this.configKeyMap.values()) {
            JsonElement element = getElementJsonObject(jsonObject, configKey.path);
            if (element != null) {
                setElementConfigKey(config, configKey, element);
            }
        }
    }

    public JsonObject saveJsonConfig(Object config) {
        this.cls.cast(config); // Verify input type
        JsonObject jsonObject = new JsonObject();
        for (ConfigKey configKey : this.configKeyMap.values()) {
            JsonElement element = getElementConfigKey(config, configKey);
            if (element != null) {
                setElementJsonObject(jsonObject, configKey.path, element);
            }
        }
        return jsonObject;
    }

    private static JsonElement getElementConfigKey(Object instance, ConfigKey configKey) {
        Object configValue = Internal.getInstanceConfigKeyImpl(instance, configKey);
        if (configValue == null) {
            return null;
        }
        if (configValue instanceof Enum) {
            configValue = ((Enum<?>) configValue).name();
        }
        if (configValue instanceof String) {
            return new JsonPrimitive((String) configValue);
        }
        if (configValue instanceof Boolean) {
            return new JsonPrimitive((Boolean) configValue);
        }
        if (configValue instanceof Number) {
            return new JsonPrimitive((Number) configValue);
        }
        throw new AssertionError("Config allow type " + configValue.getClass().getName());
    }

    private static void setElementConfigKey(Object instance, ConfigKey configKey, JsonElement jsonElement) {
        if (!jsonElement.isJsonPrimitive()) {
            return;
        }
        JsonPrimitive jsonPrimitive = (JsonPrimitive) jsonElement;
        Class<?> type = configKey.field.getType();
        Object configValue = null;
        if (jsonPrimitive.isBoolean() && type == boolean.class) {
            configValue = jsonElement.getAsBoolean();
        } else if (jsonPrimitive.isNumber() &&
                // Non char or boolean primitives are all numbers
                type != boolean.class && type != char.class && type.isPrimitive()) {
            configValue = Internal.correctNumber(jsonElement.getAsNumber(), type);
        } else if (jsonPrimitive.isString() && type == String.class) {
            configValue = jsonPrimitive.getAsString();
        }
        if (configValue != null) {
            Internal.setInstanceConfigKeyImpl(instance, configKey, configValue);
        }
    }

    private static JsonElement getElementJsonObject(JsonObject jsonObject, String path) {
        JsonObject directJsonObject = jsonObject;
        int index = 0;
        int tmpIndex;
        while ((tmpIndex = path.indexOf('.', index)) != -1) {
            String subKey = path.substring(index, tmpIndex);
            if (!directJsonObject.has(subKey)) return null;
            directJsonObject = directJsonObject.getAsJsonObject(subKey);
            index = tmpIndex + 1;
        }
        return directJsonObject.get(path.substring(index));
    }

    private static void setElementJsonObject(JsonObject jsonObject, String path, JsonElement jsonElement) {
        JsonObject directJsonObject = jsonObject;
        int index = 0;
        int tmpIndex;
        while ((tmpIndex = path.indexOf('.', index)) != -1) {
            String subKey = path.substring(index, tmpIndex);
            if (directJsonObject.has(subKey)) {
                directJsonObject = directJsonObject.getAsJsonObject(subKey);
            } else {
                JsonObject oldJsonObject = directJsonObject;
                directJsonObject = new JsonObject();
                oldJsonObject.add(subKey, directJsonObject);
            }
            index = tmpIndex + 1;
        }
        directJsonObject.add(path.substring(index), jsonElement);
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
