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
package com.fox2code.foxloader.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

    public static boolean isFieldStringCollection(Field field) {
        Class<?> type = field.getType();
        switch (type.getName()) {
            case "java.util.Collection":
            case "java.util.Set":
            case "java.util.HashSet":
            case "java.util.LinkedHashSet":
            case "java.util.List":
            case "java.util.ArrayList":
            case "java.util.LinkedList": {
                break;
            }
            default: {
                return false;
            }
        }
        String signature = field.getGenericType().getTypeName();
        int start = signature.indexOf('<');
        return start != -1 && signature.endsWith(">") &&
                signature.substring(start + 1, signature.length() - 1).equals("java.lang.String");
    }
}
