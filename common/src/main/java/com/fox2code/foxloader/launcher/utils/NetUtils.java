package com.fox2code.foxloader.launcher.utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;

public class NetUtils {
    private static final String GRADLE_USER_AGENT;

    static {
        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");
        String javaVendorVersion = System.getProperty("java.vm.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        GRADLE_USER_AGENT = String.format("Gradle/7.5.1 (%s;%s;%s) (%s;%s;%s)",
                osName, osVersion, osArch, javaVendor, javaVersion, javaVendorVersion);
    }

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

    public static byte[] hashOf(String text) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(text.getBytes(StandardCharsets.UTF_8));

        byte[] hash = digest.digest();
        if (hash.length != 32) {
            throw new AssertionError(
                    "Result hash is not the result hash of a SHA-256 hash " +
                            "(got " + hash.length + ", expected 32)");
        }
        return hash;
    }

    public static void downloadTo(String url, OutputStream outputStream) throws IOException {
        downloadTo(URI.create(url).toURL(), outputStream);
    }

    public static void downloadTo(URL url, OutputStream outputStream) throws IOException {
        if (BrowserLike.DESKTOP_MODE_DOMAINS.contains(url.getHost())) {
            // Good practice is to always say when we are a bot, unless...
            BrowserLike.downloadTo(url, outputStream);
            return;
        }
        HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        con.setConnectTimeout(5000);
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("User-Agent", GRADLE_USER_AGENT);
        try (InputStream is = con.getInputStream()) {
            byte[] byteChunk = new byte[4096];
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                outputStream.write(byteChunk, 0, n);
            }

            outputStream.flush();
        }
    }

    public static String downloadAsString(String url) throws IOException {
        return downloadAsString(URI.create(url).toURL());
    }

    public static String downloadAsString(URL url) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        downloadTo(url, byteArrayOutputStream);
        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static class BrowserLike {
        private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36";
        static final HashSet<String> DESKTOP_MODE_DOMAINS = new HashSet<>(Arrays.asList( // dsc domains
                new String(Base64.getDecoder().decode("Y2RuLmRpc2NvcmRhcHAuY29t"), StandardCharsets.UTF_8),
                new String(Base64.getDecoder().decode("bWVkaWEuZGlzY29yZGFwcC5uZXQ="), StandardCharsets.UTF_8)
        ));

        static void downloadTo(URL url, OutputStream outputStream) throws IOException {
            HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

            con.setRequestMethod("GET");
            con.setReadTimeout(15000);
            con.setConnectTimeout(15000);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("User-Agent", DESKTOP_USER_AGENT);

            int http = con.getResponseCode();

            try (InputStream is = (http >= 400 && http < 600) ?
                    con.getErrorStream() : con.getInputStream()) {
                byte[] byteChunk = new byte[4096];
                int n;

                while ((n = is.read(byteChunk)) > 0) {
                    outputStream.write(byteChunk, 0, n);
                }

                outputStream.flush();
            }
        }
    }
}
