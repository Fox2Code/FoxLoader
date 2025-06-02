package com.fox2code.foxloader.dev;

import java.text.Normalizer;

/**
 * Config for the FoxLoader gradle plugin
 */
public class FoxLoaderConfig {
    public FoxLoaderConfig() {}

    boolean decompileSources = // Only decompile sources if we have no CI to not waste server time
            System.getenv("CI") == null && System.getenv("JITPACK") == null;
    boolean addJitPackCIPublish = false;
    String username = Normalizer.normalize(System.getProperty("user.name"),
            Normalizer.Form.NFD).replaceAll("[^a-zA-Z0-9_]+","");
    public String modMain;
    public String modId = "null";
    public String modVersion;
    public String modName;
    public String modDesc;
    public String modAuthors;
    public String modIcon;
    public String modWebsite;
    public String modClassTransformer;
    public String modLoadingPlugin;

    public void modDesc() {
        if (this.modDesc == null) {
            this.modDesc = "";
        } else {
            this.modDesc += "\n";
        }
    }

    public void modDesc(String text) {
        if (this.modDesc == null) {
            this.modDesc = text;
        } else {
            this.modDesc += "\n" + text;
        }
    }

    // For testing only
    public String dumpClass;
    public String foxLoaderLibVersionOverride;
    public boolean localTesting = false;
    public boolean forceReload = false;
    public boolean unofficial = false;
    public boolean useLWJGLX = false;
    public String LWJGLXVersion = "0.21";
    public String LWJGLXLWJGLVersion = "3.3.1";
}
