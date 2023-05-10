package com.fox2code.foxloader.loader.lua;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LuaInterop {
    /**
     * @return default to use in the java class
     */
    String[] value() default {};
}
