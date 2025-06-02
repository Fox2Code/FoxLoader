package com.fox2code.foxloader.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class SourceUtil {
    private SourceUtil() {}
    public static File getSourceFile(Class<?> cls) {
        CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
        if (codeSource instanceof CodeSourceFileProvider) {
            return ((CodeSourceFileProvider) codeSource).getFile();
        }
        try {
            return new File(codeSource.getLocation().toURI().getPath()).getAbsoluteFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getSourceFileOfClassName(String cls) {
        try {
            return getSourceFile(Class.forName(cls, false, Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public interface CodeSourceFileProvider {
        File getFile();
    }
}
