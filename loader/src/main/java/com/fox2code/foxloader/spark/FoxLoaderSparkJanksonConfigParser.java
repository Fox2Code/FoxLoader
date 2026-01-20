/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.spark;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.*;
import com.fox2code.foxloader.config.ConfigIO;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.google.gson.JsonElement;
import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.ExcludedConfigFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

final class FoxLoaderSparkJanksonConfigParser implements ConfigParser {
    static final FoxLoaderSparkJanksonConfigParser INSTANCE = new FoxLoaderSparkJanksonConfigParser();

    private FoxLoaderSparkJanksonConfigParser() {}

    @Override
    public JsonElement load(String file, ExcludedConfigFilter filter) throws IOException {
        Map<String, Object> values = this.parse(Paths.get(file));
        return values == null ? null : filter.apply(ModLoaderInit.gson.toJsonTree(values));
    }

    @Override
    public Map<String, Object> parse(BufferedReader reader) throws IOException {
        HashMap<String, Object> hashMap = new HashMap<>();
        try {
            dumpConfiguration("", hashMap, Jankson.readJson(reader, ConfigIO.READER_OPTIONS));
        } catch (SyntaxError e) {
            throw new IOException("Failed to read Jankson", e);
        }

        return hashMap;
    }

    private static void dumpConfiguration(String path,  HashMap<String, Object> hashMap, ValueElement valueElement) {
        if (valueElement instanceof ObjectElement) {
            ObjectElement objectElement = (ObjectElement) valueElement;
            for (KeyValuePairElement keyValuePairElement : objectElement) {
                dumpConfiguration(path.isEmpty() ? keyValuePairElement.getKey() :
                        path + "." + keyValuePairElement.getKey(),
                        hashMap, keyValuePairElement.getValue());
            }
        } else if (valueElement instanceof PrimitiveElement) {
            hashMap.put(path, ((PrimitiveElement) valueElement)
                    .getValue().orElseThrow(NoSuchElementException::new));
        }
    }
}
