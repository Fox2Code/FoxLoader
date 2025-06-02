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
