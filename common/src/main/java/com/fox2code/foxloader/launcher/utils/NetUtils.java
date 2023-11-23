package com.fox2code.foxloader.launcher.utils;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;

public class NetUtils {
    private static final String GRADLE_USER_AGENT;
    private static final Charset DEFAULT_ENCODING;

    static {
        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");
        String javaVendorVersion = System.getProperty("java.vm.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        GRADLE_USER_AGENT = String.format("Gradle/8.4 (%s;%s;%s) (%s;%s;%s)",
                osName, osVersion, osArch, javaVendor, javaVersion, javaVendorVersion);
        DEFAULT_ENCODING = StandardCharsets.UTF_8;
    }

    public static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    @Deprecated
    public static byte[] hashOf(File file) throws IOException, NoSuchAlgorithmException {
        return IOUtils.sha256Of(file);
    }

    @Deprecated
    public static byte[] hashOf(String text) throws IOException, NoSuchAlgorithmException {
        return IOUtils.sha256Of(text);
    }

    public static void downloadTo(String url, OutputStream outputStream) throws IOException {
        downloadTo(URI.create(url).toURL(), outputStream);
    }

    public static void downloadTo(URL url, OutputStream outputStream) throws IOException {
        downloadToImpl(url, outputStream, false);
    }

    public static String downloadAsString(String url) throws IOException {
        return downloadAsString(URI.create(url).toURL());
    }

    public static String downloadAsString(URL url) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Charset charset = downloadToImpl(url, byteArrayOutputStream, true);
        return new String(byteArrayOutputStream.toByteArray(), charset);
    }

    private static Charset downloadToImpl(URL url, OutputStream outputStream, boolean findCharset) throws IOException {
        if (BrowserLike.DESKTOP_MODE_DOMAINS.contains(url.getHost())) {
            // Good practice is to always say when we are a bot, unless...
            return BrowserLike.downloadToImpl(url, outputStream, findCharset);
        }
        HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        con.setConnectTimeout(5000);
        con.setInstanceFollowRedirects(true);
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
        return findCharset ? charsetFromContentTypeImpl(con.getContentType(), true) : DEFAULT_ENCODING;
    }

    private static Charset charsetFromContentTypeImpl(String contentType, boolean allowJank) {
        if (contentType == null || contentType.isEmpty())
            return DEFAULT_ENCODING;
        int start = contentType.indexOf(";charset=");
        if (start != -1) {
            start += 9;
        } else {
            start = contentType.indexOf("; charset=");
            if (start == -1) return DEFAULT_ENCODING;
            start += 10;
        }
        start += 9;
        int end = contentType.indexOf(';', start);
        if (end == -1) end = contentType.length();
        try {
            if (contentType.charAt(start) == ' ')
                start++;
            String charset;
            if (contentType.charAt(start) == '"') {
                start++;
                if (allowJank) {
                    charset = contentType.substring(start, end);
                    if (charset.contains("\\\""))
                        return DEFAULT_ENCODING;
                    charset = charset.replace("\"", "");
                } else if (contentType.charAt(end - 1) == '"') {
                    end--;
                    charset = contentType.substring(start, end);
                } else return DEFAULT_ENCODING;
            } else charset = contentType.substring(start, end);
            return Charset.forName(charset);
        } catch (Exception ignored) {}
        return DEFAULT_ENCODING;
    }

    private static class BrowserLike {
        private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36";
        static final HashSet<String> DESKTOP_MODE_DOMAINS = new HashSet<>(Arrays.asList( // dsc domains
                new String(Base64.getDecoder().decode("Y2RuLmRpc2NvcmRhcHAuY29t"), StandardCharsets.UTF_8),
                new String(Base64.getDecoder().decode("bWVkaWEuZGlzY29yZGFwcC5uZXQ="), StandardCharsets.UTF_8)
        ));

        static Charset downloadToImpl(URL url, OutputStream outputStream, boolean findCharset) throws IOException {
            HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

            con.setRequestMethod("GET");
            con.setReadTimeout(15000);
            con.setConnectTimeout(15000);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("User-Agent", DESKTOP_USER_AGENT);
            con.setRequestProperty("Upgrade-Insecure-Requests", "1");

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

            return findCharset ? charsetFromContentTypeImpl(con.getContentType(), false) : DEFAULT_ENCODING;
        }
    }
}
