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
package com.fox2code.foxloader.config;

import blue.endless.jankson.api.Jankson;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.io.JsonReaderOptions;
import blue.endless.jankson.api.io.JsonWriterOptions;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

public final class ConfigIO {
    public static final JsonReaderOptions READER_OPTIONS = new JsonReaderOptions(
            // JsonReaderOptions.Hint.ALLOW_BARE_ROOT_OBJECT, JsonReaderOptions.Hint.ALLOW_UNQUOTED_KEYS,
            JsonReaderOptions.Hint.MERGE_DUPLICATE_OBJECTS/*, JsonReaderOptions.Hint.ALLOW_KEY_EQUALS_VALUE */);
    public static final JsonWriterOptions WRITER_OPTIONS = new JsonWriterOptions("    ",
            /*JsonWriterOptions.Hint.BARE_ROOT_OBJECT, JsonWriterOptions.Hint.UNQUOTED_KEYS,
            JsonWriterOptions.Hint.KEY_EQUALS_VALUE,*/ JsonWriterOptions.Hint.WRITE_COMMENTS,
            JsonWriterOptions.Hint.WRITE_WHITESPACE, JsonWriterOptions.Hint.WRITE_NEWLINES);
    private static final String EXTENSION = ".cfg";

    private static void readConfigurationImpl(
            ModContainer modContainer, File configFileDestination,
            ConfigStructure configStructure, Object configObject) {
        try (InputStream inputStream = Files.newInputStream(configFileDestination.toPath())) {
            ObjectElement objectElement = Jankson.readJsonObject(new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)), READER_OPTIONS);
            configStructure.loadJsonConfig(objectElement, configObject);
        } catch (Throwable t) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                    "Failed to read config file of " + modContainer.getModId(), t);
        }
    }

    private static void writeConfigurationImpl(
            ModContainer modContainer, File configFileDestination,
            ConfigStructure configStructure, Object configObject) {
        try (OutputStream outputStream = Files.newOutputStream(configFileDestination.toPath())) {
            ObjectElement objectElement = configStructure.saveJsonConfig(configObject);
            OutputStreamWriter outputStreamWriter =
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            Jankson.writeJson(objectElement, outputStreamWriter, WRITER_OPTIONS);
            outputStreamWriter.flush();
        } catch (Throwable t) {
            ModLoaderInit.getModLoaderLogger().log(Level.WARNING,
                    "Failed to save default config file of " + modContainer.getModId(), t);
        }
    }

    public static void readConfiguration(ModContainer modContainer, Object configObject) {
        if (configObject != null && !(configObject instanceof NoConfigObject)) {
            ConfigStructure configStructure = ConfigStructure.parseFromClass(configObject.getClass(), modContainer);
            File configFileDestination = new File(ModLoader.getConfigFolder(), modContainer.getModId() + EXTENSION);
            if (configFileDestination.exists()) {
                readConfigurationImpl(modContainer, configFileDestination, configStructure, configObject);
            }
        }
    }

    public static void writeConfiguration(ModContainer modContainer, Object configObject) {
        if (configObject != null && !(configObject instanceof NoConfigObject)) {
            ConfigStructure configStructure = ConfigStructure.parseFromClass(configObject.getClass(), modContainer);
            File configFileDestination = new File(ModLoader.getConfigFolder(), modContainer.getModId() + EXTENSION);
            writeConfigurationImpl(modContainer, configFileDestination, configStructure, configObject);
        }
    }

    public static void readAndUpdateConfiguration(ModContainer modContainer, Object configObject) {
        if (configObject != null && !(configObject instanceof NoConfigObject)) {
            ConfigStructure configStructure = ConfigStructure.parseFromClass(configObject.getClass(), modContainer);
            File configFileDestination = new File(ModLoader.getConfigFolder(), modContainer.getModId() + EXTENSION);
            if (configFileDestination.exists()) {
                readConfigurationImpl(modContainer, configFileDestination, configStructure, configObject);
            }
            writeConfigurationImpl(modContainer, configFileDestination, configStructure, configObject);
        }
    }

    private ConfigIO() { throw new AssertionError(); }
}
