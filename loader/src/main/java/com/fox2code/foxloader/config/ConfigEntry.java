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
    String configTranslation() default "config.${modId}.${configPath}.name";

    String configName() default "";

    String configComment() default "";

    String configPath() default "${defaultConfigPath}";

    ConfigEntryType type() default ConfigEntryType.AUTO;

    double lowerBounds() default 0;

    double upperBounds() default 1;

    /**
     * @return the name of the handler to use, for config it is called when the value is changed
     */
    String handlerName() default "";
}
