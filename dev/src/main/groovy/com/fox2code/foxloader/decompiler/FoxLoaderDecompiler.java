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
package com.fox2code.foxloader.decompiler;

import com.fox2code.foxloader.decompiler.watchdog.WatchdogTimer;
import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.dev.DevDependencyImpl;
import com.fox2code.foxloader.utils.Platform;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.lwjgl.LWJGLUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Pattern;

public final class FoxLoaderDecompiler extends SingleFileSaver implements IResultSaver {
    private static final HashMap<String, Object> options = new HashMap<>();
    private static final Pattern newLine = Pattern.compile("\\r?\\n");
    private static final Pattern colorMatch = Pattern.compile("(?<!\\\\)\\\\u00[aA]7");
    private static final HashSet<String> START_COLOR_INT_METHOD_NAMES = new HashSet<>(Arrays.asList(
            "setColorOpaque_I"
    ));
    private static final HashSet<String> START_CHAR_INT_METHOD_NAMES = new HashSet<>(Arrays.asList(
            "indexOf", "lastIndexOf"
    ));
    private static final HashSet<String> END_COLOR_INT_METHOD_NAMES = new HashSet<>(Arrays.asList(
            "drawRect", "drawHorizontalLine", "drawVerticalLine",
            "drawCenteredString", "drawString", "drawStringWithBg"
    ));

    static {
        options.put("asc", "1");
        options.put("bsm", "1");
        options.put("sef", "1");
        String javaHome = System.getProperty("java.home");
        // Workaround VineFlower being executed on java8
        if (System.getProperty("java.version").startsWith("1.8.") && javaHome != null) {
            if (javaHome.endsWith("\\jre") || javaHome.endsWith("/jre")) {
                javaHome = javaHome.substring(0, javaHome.length() - 4);
            }
            options.put("jrt", javaHome);
        } else {
            options.put("jrt", "current");
        }
        options.put("ega", "1"); // Explicit Generic Arguments
        options.put("dcc", "1"); // Decompile complex constant-dynamic expressions
        options.put("nls", "0"); // New Line Seperator
        options.put("vvm", "1"); // Verify Variable Merges
        options.put("pll", "125"); // Preferred line length
        options.put("ind", "    "); // Indent String
        options.put(FoxJavadocProvider.PROPERTY_NAME, FoxJavadocProvider.INSTANCE);
        DependencyHelper.DependencyImpl.install(DevDependencyImpl.INSTANCE);
        /*PluginSources.PLUGIN_SOURCES.add(() ->
                Collections.singletonList(new FoxLoaderDecompilerPlugin()));*/
    }

    private final Fernflower engine;
    private final WatchdogTimer watchdogTimer;

    private FoxLoaderDecompiler(File source, File destination) {
        super(destination);
        engine = new Fernflower(this, options, new PrintStreamLogger(System.out));
        engine.addLibrary(getSourceFile(FoxLoaderDecompiler.class));
        try {
            engine.addLibrary(getSourceFile(LWJGLUtil.class));
        } catch (Throwable ignored) {}
        for (DependencyHelper.Dependency dependency : DependencyHelper.commonDependencies) {
            engine.addLibrary(DependencyHelper.loadDependencyAsFile(dependency));
        }
        engine.addSource(source);
        watchdogTimer = new WatchdogTimer(true);
        watchdogTimer.setEnabled(false);
    }

    public void decompile() {
        watchdogTimer.setEnabled(false);
        try {
            engine.decompileContext();
        } finally {
            watchdogTimer.setEnabled(false);
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

    private static File getSourceFile(Class<?> cls) {
        CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
        try {

            return new File(codeSource.getLocation().toURI().getPath()).getAbsoluteFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // Main class
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Expecting 2 arguments: [LIBRARY_ROOT] [INPUT] [OUTPUT]");
            System.exit(-1);
            return;
        }
        File libraryRoot = !"default".equals(args[0]) ? new File(args[0]) :
                new File(Platform.getAppDir("minecraft"), "libraries");
        File input = new File(args[1]);
        File output = new File(args[2]);
        if (!input.exists()) {
            System.out.println("Failed to find " + input.getAbsolutePath());
            System.exit(-1);
            return;
        }
        DependencyHelper.setMCLibraryRoot(libraryRoot);
        try (FoxLoaderDecompiler decompiler = new FoxLoaderDecompiler(input, output)) {
            decompiler.decompile();
        }
        System.out.flush();
        System.exit(0);
    }

    // Content fixup helper
    public String fixUpContent(String className, String content) {
        watchdogTimer.heartbeat();
        watchdogTimer.setEnabled(true);
        String[] lines = newLine.split(content);
        StringBuilder stringBuilder = new StringBuilder(content.length() + lines.length);
        for (String line : lines) {
            line = colorMatch.matcher(line).replaceAll("§");
            int startAppendNext = 0;
            while (startAppendNext < line.length()) {
                int prevStartAppendNext = startAppendNext;
                startAppendNext = checkSubstitutions(line, stringBuilder, startAppendNext);
                if (prevStartAppendNext == startAppendNext) {
                    throw new IllegalStateException(
                            "Preventing looping code at char index " +
                                    startAppendNext + " for\n    \"" + line + "\"");
                }
            }
            stringBuilder.append("\n");
        }
        watchdogTimer.heartbeat();
        return stringBuilder.toString();
    }

    private static int checkSubstitutions(String line, StringBuilder stringBuilder, int startAppendNext) {
        int methodStart = line.indexOf('(', startAppendNext);
        if (methodStart == -1) {
            stringBuilder.append(line, startAppendNext, line.length());
            return line.length();
        }
        int methodEnd = -1;
        int indent = 0;
        for (int i = methodStart + 1; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '(') indent++;
            if (c == ')') indent--;
            if (indent == -1) {
                methodEnd = i;
                break;
            }
        }
        int numberLen = 0;
        int numberStartIndex = methodStart + 1;
        if (numberStartIndex < line.length() &&
                "-0123456789".indexOf(line.charAt(numberStartIndex + numberLen)) != -1) {
            do {
                numberLen++;
                if ((numberStartIndex + numberLen) == line.length()) {
                    // If number is going to the end of the line, ignore it.
                    methodEnd = -1;
                    numberLen = 0;
                    break;
                }
            } while ("0123456789".indexOf(line.charAt(numberStartIndex + numberLen)) != -1);
        }
        if (numberLen == 1 && (line.charAt(numberStartIndex) == '-' ||
                line.charAt(numberStartIndex + numberLen) == 'x')) {
            numberLen = 0; // Skip start number if already hexadecimal
        }
        int endNumberLen = 0;
        if (methodEnd != -1) {
            int tmp;
            while ((tmp = "-0123456789".indexOf(line.charAt((methodEnd - 1) - endNumberLen))) != -1) {
                endNumberLen++;
                if (tmp == 0) {
                    break;
                }
            }
            if (endNumberLen != 0 && line.charAt(methodEnd - endNumberLen - 1) == 'x') {
                endNumberLen = 0; // Skip end number if already hexadecimal
            }
        }
        int endNumberStartIndex = methodEnd - endNumberLen;
        int methodDot = line.lastIndexOf('.', methodStart);
        String methodName = line.substring(methodDot + 1, methodStart).trim();
        if (numberLen != 0 && startAppendNext < numberStartIndex) {
            if (START_COLOR_INT_METHOD_NAMES.contains(methodName)) {
                //noinspection DuplicateExpressions
                int number = Integer.parseInt(line.substring(
                        numberStartIndex, numberStartIndex + numberLen));
                stringBuilder.append(line, startAppendNext, numberStartIndex)
                        .append(String.format("0x%08X", number));
                startAppendNext = numberStartIndex + numberLen;
            } else if (START_CHAR_INT_METHOD_NAMES.contains(methodName)) {
                //noinspection DuplicateExpressions
                int number = Integer.parseInt(line.substring(
                        numberStartIndex, numberStartIndex + numberLen));
                stringBuilder.append(line, startAppendNext, numberStartIndex)
                        .append('\'');
                appendEscapedChar(stringBuilder, (char) number);
                stringBuilder.append('\'');
                startAppendNext = numberStartIndex + numberLen;
            }
        }
        if (END_COLOR_INT_METHOD_NAMES.contains(methodName) &&
                endNumberLen != 0 && startAppendNext < endNumberStartIndex) {
            int number = Integer.parseInt(line.substring(
                    endNumberStartIndex, endNumberStartIndex + endNumberLen));
            stringBuilder.append(line, startAppendNext, endNumberStartIndex)
                    .append(String.format("0x%08X", number));
            startAppendNext = endNumberStartIndex + endNumberLen;
        }
        if (startAppendNext <= methodStart) {
            // Avoid infinite looping over every method.
            stringBuilder.append(line, startAppendNext, methodStart + 1);
            startAppendNext = methodStart + 1;
        }
        return startAppendNext;
    }

    private static void appendEscapedChar(StringBuilder stringBuilder, char c) {
        switch (c) {
            case '\u0000': stringBuilder.append("\\u0000"); break;
            case '\b': stringBuilder.append("\\b"); break;
            case '\t': stringBuilder.append("\\t"); break;
            case '\n': stringBuilder.append("\\n"); break;
            case '\f': stringBuilder.append("\\f"); break;
            case '\r': stringBuilder.append("\\r"); break;
            case '\"': stringBuilder.append("\\\""); break;
            case '\'': stringBuilder.append("\\'"); break;
            case '\\': stringBuilder.append("\\\\"); break;
            default: stringBuilder.append(c); break;
        }
    }
}