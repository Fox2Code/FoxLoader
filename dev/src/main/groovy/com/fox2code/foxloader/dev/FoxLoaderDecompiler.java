package com.fox2code.foxloader.dev;

import com.fox2code.foxloader.launcher.utils.SourceUtil;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.lwjgl.LWJGLUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FoxLoaderDecompiler extends SingleFileSaver implements IBytecodeProvider, IResultSaver {
    private static final HashMap<String, Object> options = new HashMap<>();

    static {
        options.put("asc", "1");
        options.put("bsm", "1");
        options.put("sef", "1");
        options.put("jrt", "1");
        options.put("ega", "1");
        options.put("nls", "0");
        options.put("pll", "125");
        options.put("ind", "    ");
    }

    private final Fernflower engine;

    public FoxLoaderDecompiler(File source, File destination, boolean client) {
        super(destination);
        engine = new Fernflower(this, this, options, new PrintStreamLogger(System.out));
        engine.addLibrary(SourceUtil.getSourceFile(FoxLoaderDecompiler.class));
        if (client) {
            engine.addLibrary(SourceUtil.getSourceFile(LWJGLUtil.class));
        }
        engine.addSource(source);
    }

    public void decompile() {
        engine.decompileContext();
    }

    public String fixUpContent(String className, String content) {
        // TODO Hexify color int.
        return content;
    }

    // *******************************************************************
    // Interface IBytecodeProvider
    // *******************************************************************

    @Override
    public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
        File file = new File(externalPath);
        if (internalPath == null) {
            return InterpreterUtil.getBytes(file);
        }
        else {
            try (ZipFile archive = new ZipFile(file)) {
                ZipEntry entry = archive.getEntry(internalPath);
                if (entry == null) throw new IOException("Entry not found: " + internalPath);
                return InterpreterUtil.getBytes(archive, entry);
            }
        }
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
