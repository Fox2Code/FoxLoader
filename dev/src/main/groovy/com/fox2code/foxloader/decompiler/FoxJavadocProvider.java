package com.fox2code.foxloader.decompiler;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

public final class FoxJavadocProvider implements IFabricJavadocProvider {
    public static final FoxJavadocProvider INSTANCE = new FoxJavadocProvider();
    private static final HashMap<String, String> methodsJavaDocs = new HashMap<>();

    private FoxJavadocProvider() {}

    static {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(FoxLoaderDecompiler.class.getResourceAsStream("javadocs.txt")),
                StandardCharsets.UTF_8))) {
            String line;

            final StringBuilder stringBuilder = new StringBuilder();
            String key = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("# ")) {
                    if (key != null) {
                        addJavadoc(key, stringBuilder);
                    }
                    stringBuilder.setLength(0);
                    key = line.substring(2);
                    continue;
                }
                stringBuilder.append(line).append('\n');
            }
            if (key != null) {
                addJavadoc(key, stringBuilder);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private static void addJavadoc(String key, StringBuilder javaDoc) {
        int end = javaDoc.length();
        while (javaDoc.charAt(end - 1) < ' ') {
            end--;
        }
        javaDoc.setLength(end);
        methodsJavaDocs.put(key, javaDoc.toString());
    }

    @Override
    public String getClassDoc(StructClass structClass) {
        return null;
    }

    @Override
    public String getFieldDoc(StructClass structClass, StructField structField) {
        return null;
    }

    @Override
    public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
        return methodsJavaDocs.get(structClass.qualifiedName + "." + structMethod.getName() + "()");
    }
}
