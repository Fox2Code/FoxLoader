package com.fox2code.foxloader.utils.io;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IOUtils {
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] byteChunk = new byte[4096];
        int n;

        while ((n = inputStream.read(byteChunk)) > 0) {
            outputStream.write(byteChunk, 0, n);
        }
    }

    public static void copyAndClose(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (InputStream is = inputStream;
             OutputStream out = outputStream) {
            byte[] byteChunk = new byte[4096];
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                out.write(byteChunk, 0, n);
            }
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copyAndClose(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static MessageDigest getSHA256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Missing SHA-256, is the JVM corrupted?", e);
        }
    }

    public static byte[] sha256Of(File file) throws IOException {
        return sha256Of(Files.newInputStream(file.toPath()));
    }

    public static byte[] sha256Of(URL url) throws IOException {
        return sha256Of(url.openStream());
    }

    private static byte[] sha256Of(InputStream in) throws IOException {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = getSHA256Digest();
        try (BufferedInputStream bis = new BufferedInputStream(in)) {
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

    public static byte[] sha256Of(String text) {
        MessageDigest digest = getSHA256Digest();
        digest.update(text.getBytes(StandardCharsets.UTF_8));

        byte[] hash = digest.digest();
        if (hash.length != 32) {
            throw new AssertionError(
                    "Result hash is not the result hash of a SHA-256 hash " +
                            "(got " + hash.length + ", expected 32)");
        }
        return hash;
    }
}
