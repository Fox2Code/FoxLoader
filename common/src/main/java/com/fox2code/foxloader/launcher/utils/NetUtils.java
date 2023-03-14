package com.fox2code.foxloader.launcher.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class NetUtils {
    public static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    public static byte[] hashOf(File file) throws IOException, NoSuchAlgorithmException {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            while ((count = bis.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
        }

        byte[] hash = digest.digest();
        if (hash.length != 32) {
            throw new AssertionError(
                    "Result hash is not the result hash of a SHA-256 hash " +
                            "(got " + hash.length + ", expected 32)");
        }
        return hash;
    }
}
