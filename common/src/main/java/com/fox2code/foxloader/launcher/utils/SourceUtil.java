package com.fox2code.foxloader.launcher.utils;

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
}
