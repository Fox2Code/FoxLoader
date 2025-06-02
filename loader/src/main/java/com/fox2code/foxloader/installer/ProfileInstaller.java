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
package com.fox2code.foxloader.installer;

import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.utils.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ProfileInstaller {
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
            } else {
                profile.addProperty("gameDir", "./.reindev");
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
