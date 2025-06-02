package com.fox2code.foxloader.spark;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.PrimitiveElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.impl.document.BooleanElementImpl;
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
            hashMap.put(path, ((BooleanElementImpl) valueElement)
                    .getValue().orElseThrow(NoSuchElementException::new));
        }
    }
}
