package com.fox2code.foxloader.launcher;

/**
 * FoxLoader class loader marker that can be used by compatibility mods
 */
public interface ClassLoaderMarker {
    ClassLoader getClassLoader();
}
