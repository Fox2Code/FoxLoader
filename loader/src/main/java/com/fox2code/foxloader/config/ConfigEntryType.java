package com.fox2code.foxloader.config;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;

public enum ConfigEntryType {
    AUTO {
        @Override
        public boolean isValidField(Field field) {
            return false;
        }
    },
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
            return cls == String.class || cls == File.class ||
                    cls == URL.class || cls == URI.class ||
                    cls == Void.class || cls == Runnable.class;
        }
    };

    public abstract boolean isValidField(Field field);
}