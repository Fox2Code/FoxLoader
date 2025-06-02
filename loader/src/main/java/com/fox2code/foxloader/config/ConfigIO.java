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
