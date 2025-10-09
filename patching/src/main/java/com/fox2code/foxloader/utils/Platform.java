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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

public enum Platform {
    WINDOWS(new String[]{"lwjgl.dll", "lwjgl64.dll", "OpenAL32.dll", "OpenAL64.dll"}, "start", "\\bin\\java.exe"),
    MACOS(new String[]{"liblwjgl.jnilib", "openal.dylib"}, "open", "/bin/java"),
    LINUX(new String[]{"liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so"}, "xdg-open", "/bin/java");

    private static final Platform platform;

    static {
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (name.startsWith("win")) {
            platform = WINDOWS;
        } else if (name.startsWith("mac") ||
                name.startsWith("darwin")) {
            platform = MACOS;
        } else if (name.contains("nix") ||
                name.contains("nux") ||
                name.contains("aix")) {
            platform = LINUX;
        } else {
            throw new Error("Unsupported system");
        }
    }

    private final String[] natives;
    public final String open;
    public final File javaBin;

    Platform(String[] natives, String open, String javaBin) {
        this.natives = natives;
        this.open = open;
        this.javaBin = new File(System.getProperty("java.home") + javaBin).getAbsoluteFile();
    }

    public static Platform getPlatform() {
        return platform;
    }

    public void setupLwjgl2(ClassLoader classLoader, File nativesFolder) {
        if (classLoader.getResource(natives[0]) != null &&
                System.getProperty("org.lwjgl.librarypath") == null) {
            if (!nativesFolder.exists() && !nativesFolder.mkdirs()) return;
            for (String lib : this.natives) {
                File file = new File(nativesFolder, lib);
                if (file.exists()) continue;
                try (InputStream inputStream = classLoader.getResourceAsStream(lib)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, file.toPath());
                    }
                } catch (IOException ignored) {}
            }
            System.setProperty("org.lwjgl.librarypath", nativesFolder.getPath());
        }
    }

    public static File getAppDir(String baseDir) {
        String homeDir = System.getProperty("user.home", ".");
        File file;
        switch(platform) {
            case LINUX:
                file = new File(homeDir, '.' + baseDir + '/');
                break;
            case WINDOWS:
                String appdata = System.getenv("APPDATA");
                if (appdata != null) {
                    file = new File(appdata, "." + baseDir + '/');
                } else {
                    file = new File(homeDir, '.' + baseDir + '/');
                }
                break;
            case MACOS:
                file = new File(homeDir, "Library/Application Support/" + baseDir);
                break;
            default:
                file = new File(homeDir, baseDir + '/');
        }

        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + file);
        } else {
            return file;
        }
    }

    private static final int jvmVersion = getJvmVersion0();
    private static final boolean jvm32Bit =
            "32".equals(System.getProperty("sun.arch.data.model"));

    private static int getJvmVersion0() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf('.');
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

    public static int getJvmVersion() {
        return jvmVersion;
    }

    public static boolean isJVM32Bit() {
        return jvm32Bit;
    }
}
