package com.fox2code.foxloader.launcher.utils;

import com.fox2code.foxloader.launcher.FoxLauncher;

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

    public void setupLwjgl2() {
        if (FoxLauncher.getFoxClassLoader().getResource(natives[0]) != null &&
                System.getProperty("org.lwjgl.librarypath") == null) {
            File natives = new File(FoxLauncher.getGameDir(), "natives");
            if (!natives.exists() && !natives.mkdirs()) return;
            for (String lib : this.natives) {
                File file = new File(natives, lib);
                if (file.exists()) continue;
                try (InputStream inputStream = FoxLauncher.getFoxClassLoader().getResourceAsStream(lib)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, file.toPath());
                    }
                } catch (IOException ignored) {}
            }
            System.setProperty("org.lwjgl.librarypath", natives.getPath());
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
}
