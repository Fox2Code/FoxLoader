package com.fox2code.foxloader.internal;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import net.minecraft.common.util.i18n.StringTranslate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;

public final class InternalTranslateHooks {
    private static final boolean initialized = false;
    public static final Properties fallbackTranslations = new Properties();
    private static final HashMap<String, Properties> translationsCache = new HashMap<>();
    private static final Function<String, Properties> translationsCacheFiller = lang -> {
        Properties properties = new Properties();
        for (ModContainer modContainer : ModLoaderInit.getModContainers()) {
            loadLanguageTo(modContainer, lang, properties);
        }
        return properties;
    };

    private InternalTranslateHooks() {}

    private static void loadLanguageTo(ModContainer modContainer, String lang, Properties target) {
        try (InputStream inputStream = ModContainer.class.getResourceAsStream(
                "/assets/" + modContainer.getModId() + "/lang/" + lang + ".lang")) {
            if (inputStream != null) {
                target.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {}
    }

    static {
        translationsCache.put("en_US", InternalTranslateHooks.fallbackTranslations);
    }

    public static Properties getTranslationsForLanguage(String lang) {
        if (!ModLoader.areAllModsLoaded()) return InternalTranslateHooks.fallbackTranslations;
        return translationsCache.computeIfAbsent(lang, translationsCacheFiller);
    }

    public static void notifyInit() {
        for (ModContainer modContainer : ModLoaderInit.getModContainers()) {
            loadLanguageTo(modContainer, "en_US", InternalTranslateHooks.fallbackTranslations);
        }
        StringTranslate.reloadKeys();
    }
}
