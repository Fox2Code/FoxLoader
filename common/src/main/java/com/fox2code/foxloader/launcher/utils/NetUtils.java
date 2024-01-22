package com.fox2code.foxloader.launcher.utils;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

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
        downloadToImpl(url, outputStream, null, false);
    }

    public static String downloadAsString(String url) throws IOException {
        return downloadAsString(URI.create(url).toURL());
    }

    public static String downloadAsString(URL url) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Charset charset = downloadToImpl(url, byteArrayOutputStream, null, true);
        try {
            return new String(byteArrayOutputStream.toByteArray(), charset);
        } catch (Exception e) {
            throw new IOException("Failed to decode string with charset", e);
        }
    }

    public static String postRequest(URL url, String post) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Charset charset = downloadToImpl(url, byteArrayOutputStream, post == null ? "" : post, true);
        try {
            return new String(byteArrayOutputStream.toByteArray(), charset);
        } catch (Exception e) {
            throw new IOException("Failed to decode string with charset", e);
        }
    }

    private static Charset downloadToImpl(URL url, OutputStream outputStream, String postData, boolean findCharset) throws IOException {
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
        return findCharset ? charsetFromContentTypeImpl(con.getContentType()) : DEFAULT_ENCODING;
    }

    private static Charset charsetFromContentTypeImpl(String contentType) {
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
                charset = contentType.substring(start, end);
                if (charset.contains("\\\""))
                    return DEFAULT_ENCODING;
                charset = charset.replace("\"", "");
            } else charset = contentType.substring(start, end);
            return Charset.forName(charset);
        } catch (Exception ignored) {}
        return DEFAULT_ENCODING;
    }
}
