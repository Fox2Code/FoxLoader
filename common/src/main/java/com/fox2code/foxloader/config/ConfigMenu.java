package com.fox2code.foxloader.config;

import java.util.List;

public class ConfigMenu {
    public final String menuName;
    public final String menuTranslation;
    public final List<ConfigKey> configKeys;

    public ConfigMenu(String menuName, String menuTranslation, List<ConfigKey> configKeys) {
        this.menuName = menuName;
        this.menuTranslation = menuTranslation;
        this.configKeys = configKeys;
    }
}
