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
package com.fox2code.foxloader.loader.contributors;

import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

public final class Contributors {
    private static final HashSet<String> contributorsUUIDs = new HashSet<>();
    private static final HashSet<String> contributorsNames = new HashSet<>();

    static {
        // If your name is not there, and you contributed, just open an issue on GitHub
        addContributor("a5adabf9-0c1f-4d03-855b-61e334cd96d7", "Fox2Code");
        addContributor("76982056-c381-46f6-ab25-2415e1e4d554", "kivattt");
        addContributor("898febf0-4bd0-4a77-892c-2b1cbf534830", "_Dereku");
        addContributor("66580d8e-19ad-4564-a8f1-448d021be321", "Chocohead");
        addContributor("4a23f5f2-03ce-41e3-bad4-cfa827d13458", "Halotroop2288");
        addContributor("8d8bad89-8b05-47b6-b73f-0bbe400c2ec0", "jktechh");
        addContributor("e6f83bcf-6cc0-4af7-a1ea-4c39342e4be2", "Silveros");
    }

    private static void addContributor(String uuid, String name) {
        contributorsUUIDs.add(uuid);
        contributorsNames.add(name.toLowerCase(Locale.ROOT));
    }

    public static boolean hasContributorUUID(UUID uuid) {
        return contributorsUUIDs.contains(uuid.toString());
    }

    public static boolean hasContributorUUID(String uuid) {
        return contributorsUUIDs.contains(uuid);
    }

    public static boolean hasContributorName(String name) {
        return contributorsNames.contains(name.toLowerCase(Locale.ROOT));
    }
}