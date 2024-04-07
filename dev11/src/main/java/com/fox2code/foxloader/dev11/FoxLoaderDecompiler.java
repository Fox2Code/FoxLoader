package com.fox2code.foxloader.dev11;

import com.fox2code.foxloader.dev.IFoxLoaderDecompiler;
import com.fox2code.foxloader.launcher.utils.SourceUtil;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.lwjgl.LWJGLUtil;

import java.io.File;
import java.util.HashMap;

public class FoxLoaderDecompiler extends SingleFileSaver implements IResultSaver, IFoxLoaderDecompiler {
    private static final HashMap<String, Object> options = new HashMap<>();

    static {
        options.put("asc", "1");
        options.put("bsm", "1");
        options.put("sef", "1");
        String javaHome = System.getProperty("java.home");
        if (System.getProperty("java.version").startsWith("1.8.") && javaHome != null) {
            if (javaHome.endsWith("\\jre") || javaHome.endsWith("/jre")) {
                javaHome = javaHome.substring(0, javaHome.length() - 4);
            }
            options.put("jrt", javaHome);
        } else {
            options.put("jrt", "current");
        }
        options.put("ega", "1");
        options.put("dcc", "1");
        options.put("nls", "0");
        options.put("pll", "125");
        options.put("ind", "    ");
    }

    private final Fernflower engine;

    public FoxLoaderDecompiler(File source, File destination, boolean client) {
        super(destination);
        engine = new Fernflower(this, options, new PrintStreamLogger(System.out));
        engine.addLibrary(SourceUtil.getSourceFile(FoxLoaderDecompiler.class));
        if (client) {
            try {
                engine.addLibrary(SourceUtil.getSourceFile(LWJGLUtil.class));
            } catch (Throwable ignored) {}
        }
        engine.addSource(source);
    }

    @Override
    public void decompile() {
        engine.decompileContext();
    }

    public String fixUpContent(String className, String content) {
        // TODO Hexify color int.
        return content;
    }

    // *******************************************************************
    // Interface IResultSaver
    // *******************************************************************

    @Override
    public synchronized void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content, int[] mapping) {
        if (entryName.endsWith(".java")) {
            content = fixUpContent(entryName.substring(0, entryName.length() - 5).replace('/', '.'), content);
        }
        super.saveClassEntry(path, archiveName, qualifiedName, entryName, content, mapping);
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        super.saveClassFile(path, qualifiedName, entryName,
                fixUpContent(qualifiedName.replace('/', '.'), content), mapping);
    }
}
