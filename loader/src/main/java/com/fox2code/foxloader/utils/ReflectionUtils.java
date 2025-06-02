package com.fox2code.foxloader.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public final class ReflectionUtils {
    private ReflectionUtils() {}

    public static <T> Constructor<T> getPublicConstructorSafeMulti(Class<T> tClass, Class<?>[]... cstArgs) {
        for (Class<?>[] cstArg : cstArgs) {
            Constructor<T> constructor = getPublicConstructorSafe(tClass, cstArg);
            if (constructor != null) {
                return constructor;
            }
        }
        return null;
    }

    public static <T> Constructor<T> getPublicConstructorSafe(Class<T> tClass, Class<?>... args) {
        try {
            Constructor<T> constructor = tClass.getDeclaredConstructor(args);
            if (Modifier.isPublic(constructor.getModifiers())) {
                return constructor;
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    public static <T> T invokeConstructorSafe(Constructor<T> constructor, Object... args) {
        if (constructor == null || !(constructor.isAccessible() || Modifier.isPublic(constructor.getModifiers()))) {
            return null;
        }
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException targetException) {
            Throwable cause = targetException.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }
}
