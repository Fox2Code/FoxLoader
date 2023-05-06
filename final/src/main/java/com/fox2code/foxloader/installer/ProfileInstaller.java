package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.utils.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileInstaller {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    public static void install(File file) throws IOException {
        JsonObject jsonObject = null;
        if (file.exists()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(
                    Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
                jsonObject = gson.fromJson(inputStreamReader, JsonObject.class);
            }
        }
        if (jsonObject == null) {
            jsonObject = new JsonObject();
        }
        JsonObject profiles;
        if (!jsonObject.has("profiles")) {
            jsonObject.add("profiles", profiles = new JsonObject());
        } else {
            profiles = jsonObject.get("profiles").getAsJsonObject();
        }
        JsonObject profile;
        final String profileKey = "ReIndev-" + BuildConfig.REINDEV_VERSION + "-FoxLoader";
        final String profileDate = format.format(new Date(System.currentTimeMillis()));
        if (!profiles.has(profileKey)) {
            profiles.add(profileKey, profile = new JsonObject());
            profile.addProperty("created", profileDate);
            profile.addProperty("icon", "Bookshelf");
            if (!Main.isPojavLauncherHome(System.getProperty("user.home"))) {
                profile.addProperty("gameDir", Platform.getAppDir("reindev").getAbsolutePath());
                profile.addProperty("javaArgs", Main.optJvmArgsWithMem);
            } else {
                profile.addProperty("gameDir", "./.reindev");
                profile.addProperty("javaArgs", Main.optJvmArgs);
            }
            profile.addProperty("type", "custom");
        } else {
            profile = profiles.get(profileKey).getAsJsonObject();
        }
        profile.addProperty("lastUsed", profileDate);
        profile.addProperty("lastVersionId", InstallerGUI.DEFAULT_VERSION_NAME);
        profile.addProperty("name", "FoxLoader " + BuildConfig.FOXLOADER_VERSION);

        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            gson.toJson(jsonObject, outputStreamWriter);
        }
    }
}
