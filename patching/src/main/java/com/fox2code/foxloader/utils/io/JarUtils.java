package com.fox2code.foxloader.utils.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarUtils {
    public static Manifest getManifest(File file, String jarPath) throws IOException {
        Manifest manifest;
        if (jarPath == null) {
            try (JarFile jarFile = new JarFile(file)) {
                manifest = jarFile.getManifest();
            }
        } else {
            manifest = new Manifest();
            try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(
                    new URL("jar:" + file.toURI().toURL() +"!/" + jarPath).openStream())))  {
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    if ("META-INF/MANIFEST.MF".equals(zipEntry.getName())) {
                        manifest.read(zipInputStream);
                        break;
                    }
                }
            }
        }
        return manifest;
    }
}
