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

import java.util.logging.Level;

public final class CustomLogLevel extends Level {
    public static final Level STDOUT = new CustomLogLevel("STDOUT", 800);
    public static final Level STDERR = new CustomLogLevel("STDERR", 1000);
    public static final Level TRACE = new CustomLogLevel("TRACE", 300);
    public static final Level DEBUG = new CustomLogLevel("DEBUG", 500);
    public static final Level ERROR = new CustomLogLevel("ERROR", 1000);

    public CustomLogLevel(String name, int value) {
        super(name, value, INFO.getResourceBundleName());
        if (name.length() > 7) {
            throw new IllegalArgumentException("Maximum length of a custom log level is 7!");
        }
    }
}
