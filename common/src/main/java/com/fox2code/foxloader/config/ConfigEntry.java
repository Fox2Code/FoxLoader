package com.fox2code.foxloader.config;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {
    String configTranslation() default "config.${modId}.${configPath}.config";

    String configName() default "";

    String configPath() default "${defaultConfigPath}";

    ConfigEntryType type() default ConfigEntryType.CONFIG;

    double lowerBounds() default 0;

    double upperBounds() default 1;

    /**
     * @return if annotation should be runtime only.
     */
    boolean runtimeOnly() default true;

    /**
     * @return the name of the handler to use, for config it is called when the value is changed
     */
    String handlerName() default "";

    enum ConfigEntryType {

        CONFIG {
            @Override
            public boolean isValidField(Field field) {
                Class<?> cls = field.getType();
                return cls.isEnum() || (cls.isPrimitive() && cls != char.class) || cls == String.class;
            }
        },
        SUBMENU {
            @Override
            public boolean isValidField(Field field) {
                Class<?> cls = field.getType();
                return cls != field.getDeclaringClass() &&
                        !cls.getName().startsWith("java.lang.");
            }
        },
        LINK {
            @Override
            public boolean isValidField(Field field) {
                Class<?> cls = field.getType();
                return cls == String.class || cls == File.class || cls == URL.class || cls == URI.class;
            }
        };

        public abstract boolean isValidField(Field field);
    }
}
