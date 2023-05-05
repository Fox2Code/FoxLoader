package com.fox2code.foxloader.installer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

// We cannot use ReIndev translation engine here, make our own instead.
public class TranslateEngine {
    private static final Properties en_US = new Properties();
    private static final String[] languageList = new String[]{ "en_US", "ru_RU" };
    private static final HashMap<String, Properties> languages = new HashMap<>();
    private static final LinkedHashMap<String, String> languagesNames = new LinkedHashMap<>();
    private static final HashMap<String, WeakHashMap<Object, BiConsumer<Object, String>>> translationHandlers = new HashMap<>();
    private static final Function<String, WeakHashMap<Object, BiConsumer<Object, String>>> translationHandlersFiller = k -> new WeakHashMap<>();
    private static final BiConsumer<JLabel, String> translationLabelHandler = JLabel::setText;
    private static final BiConsumer<AbstractButton, String> translationButtonHandler = AbstractButton::setText;
    private static final BiConsumer<JTextComponent, String> translationTextComponentHandler = JTextComponent::setText;
    private static final BiConsumer<TitledBorder, String> translationTitledBorderComponentHandler = TitledBorder::setTitle;
    private static final WeakHashMap<Window, Void> translationWindowPackHandler = new WeakHashMap<>();

    private static Properties currentLang = en_US;
    private static String currentLangId = "en_US";

    static {
        try {
            loadLang(en_US, "en_US");
        } catch (IOException e) {
            throw new IOError(e);
        }
        for (String lang : languageList) {
            Properties loadedLanguage;
            if ("en_US".equals(lang)) {
                loadedLanguage = en_US;
            } else {
                loadedLanguage = new Properties();
                try {
                    loadLang(loadedLanguage, lang);
                } catch (IOException e) {
                    System.out.println("Failed to load " + lang + " language");
                    e.printStackTrace(System.out);
                    continue;
                }
            }
            String languageName = loadedLanguage.getProperty("installer.lang");
            if (languageName == null) {
                System.out.println("Failed to load " + lang + " cause it has no name??? WTF!");
                continue;
            }
            languages.put(lang, loadedLanguage);
            languagesNames.put(languageName, lang);
        }
    }

    private static void loadLang(Properties properties, String lang) throws IOException {
        try {
            properties.load(new InputStreamReader(Objects.requireNonNull(TranslateEngine.class.getResourceAsStream(
                    "/assets/foxloader/lang/" + lang + ".lang")), StandardCharsets.UTF_8));
        } catch (NullPointerException npe) {
            throw (IOException) new FileNotFoundException(lang + ".lang").initCause(npe);
        }
    }

    public static String getTranslationFormat(String translationKey, String... args) {
        return String.format(getTranslation(translationKey), (Object[]) args);
    }

    public static String getTranslation(String translationKey) {
        if (translationKey.endsWith(".*")) {
            translationKey = translationKey.substring(0, translationKey.length() - 2);
            final String translationKeyInitial = translationKey + ".1";
            Properties language;
            if (currentLang.containsKey(translationKeyInitial)) {
                language = currentLang;
            } else if (en_US.containsKey(translationKeyInitial)) {
                language = en_US;
            } else return translationKeyInitial;
            return getTranslationMulti(language, translationKey);
        }
        if (currentLang.containsKey(translationKey)) {
            return currentLang.getProperty(translationKey);
        } else if (en_US.containsKey(translationKey)) {
            return en_US.getProperty(translationKey);
        } else return translationKey;
    }

    private static String getTranslationMulti(Properties lang, String translationBase) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            String translate = lang.getProperty(translationBase + "." + i);
            if (translate == null) break;
            stringBuilder.append(translate).append('\n');
        }
        final int len = stringBuilder.length();
        if (len != 0) {
            stringBuilder.setLength(len - 1);
        }
        return stringBuilder.toString();
    }

    public static void updateOnTranslate(Window window) {
        translationWindowPackHandler.put(window, null);
    }

    public static void installOn(JLabel label, String translationKey) {
        registerTranslationHandler(translationKey, label, translationLabelHandler);
    }

    public static void installOn(JTextComponent textComponent, String translationKey) {
        registerTranslationHandler(translationKey, textComponent, translationTextComponentHandler);
    }

    public static void installOn(AbstractButton button, String translationKey) {
        registerTranslationHandler(translationKey, button, translationButtonHandler);
    }

    public static void installOn(TitledBorder titledBorder, String translationKey) {
        registerTranslationHandler(translationKey, titledBorder, translationTitledBorderComponentHandler);
    }

    public static void installOnFormat(JLabel label, String translationKey, final String... args) {
        registerTranslationHandler(translationKey, label, (tLabel, text) -> {
            tLabel.setText(String.format(text, (Object[]) args));
        });
    }

    public static void installOnFormat(AbstractButton button, String translationKey, final String... args) {
        registerTranslationHandler(translationKey, button, (tButton, text) -> {
            tButton.setText(String.format(text, (Object[]) args));
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerTranslationHandler(String translationKey, T holder, BiConsumer<T, String> handler) {
        translationHandlers.computeIfAbsent(translationKey, translationHandlersFiller)
                .put(holder, (BiConsumer<Object, String>) handler);
        handler.accept(holder, getTranslation(translationKey));
    }

    public static TitledBorder createTitledBorder(String translationKey) {
        TitledBorder titledBorder = new TitledBorder(translationKey);
        installOn(titledBorder, translationKey);
        return titledBorder;
    }

    public static JComponent makeLanguageSelectComponent() {
        final String[] keys = languagesNames.keySet().toArray(new String[0]);
        JComboBox<String> jComboBox = new JComboBox<>(keys);
        jComboBox.addActionListener(e -> {
            String langName = keys[jComboBox.getSelectedIndex()];
            switchToLanguage(languagesNames.get(langName));
        });
        return jComboBox;
    }

    private static void switchToLanguage(String language) {
        if (language == null || currentLangId.equals(language)) {
            return;
        }
        Properties newLanguage = languages.get(language);
        if (newLanguage == null) {
            System.out.println("Language " + language + " is missing?");
            return;
        }
        currentLang = newLanguage;
        currentLangId = language;
        for (Map.Entry<String, WeakHashMap<Object, BiConsumer<Object, String>>> entry : translationHandlers.entrySet()) {
            String translated = getTranslation(entry.getKey());
            for (Map.Entry<Object, BiConsumer<Object, String>> entry2 : entry.getValue().entrySet()) {
                entry2.getValue().accept(entry2.getKey(), translated);
            }
        }
        for (Window window : translationWindowPackHandler.keySet()) {
            window.pack();
        }
    }
}
