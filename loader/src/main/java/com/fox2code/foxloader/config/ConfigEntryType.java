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