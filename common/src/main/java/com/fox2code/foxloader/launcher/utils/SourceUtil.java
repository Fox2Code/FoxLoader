package com.fox2code.foxloader.launcher.utils;

import com.fox2code.foxloader.launcher.FoxLauncher;

import java.io.File;
import java.net.URISyntaxException;

public class SourceUtil {
    private SourceUtil() {}
    public static File getSourceFile(Class<?> cls) {
        try {
            return new File(cls.getProtectionDomain().getCodeSource()
                    .getLocation().toURI().getPath()).getAbsoluteFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getSourceFileOfClassName(String cls) {
        try {
            return getSourceFile(Class.forName(cls, false, FoxLauncher.getFoxClassLoader()));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
