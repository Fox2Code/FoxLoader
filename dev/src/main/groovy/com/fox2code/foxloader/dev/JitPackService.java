package com.fox2code.foxloader.dev;

public enum JitPackService {
    GITHUB("https://github.com/", "com.github."),
    GITLAB("https://gitlab.com/", "com.gitlab.");

    public final String websitePrefix;
    public final String jitpackPrefix;

    JitPackService(String websitePrefix, String jitpackPrefix) {
        this.websitePrefix = websitePrefix;
        this.jitpackPrefix = jitpackPrefix;
    }
}
