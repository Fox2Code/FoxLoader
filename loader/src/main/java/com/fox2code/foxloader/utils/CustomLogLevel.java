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
