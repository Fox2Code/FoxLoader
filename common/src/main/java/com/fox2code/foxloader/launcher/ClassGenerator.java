package com.fox2code.foxloader.launcher;

import java.net.URL;

/**
 * Used to generate new classes, only called when the class loader fails to load any class
 */
@FunctionalInterface
public interface ClassGenerator {
    byte[] generate(String className);

    default URL source(String className) { return null; }
}
