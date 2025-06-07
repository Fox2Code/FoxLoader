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
package com.fox2code.foxloader.utils.io;


import javax.net.ssl.HttpsURLConnection;
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
        GRADLE_USER_AGENT = String.format("Gradle/8.8 (%s;%s;%s) (%s;%s;%s)",
                osName, osVersion, osArch, javaVendor, javaVersion, javaVendorVersion);
        DEFAULT_ENCODING = StandardCharsets.UTF_8;
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
        if (con instanceof HttpsURLConnection) {
            try {
                CertificateHelper.install((HttpsURLConnection) con);
            } catch (Exception e) {
                throw new IOException("Failed to install CertificateHelper", e);
            }
        }
        if (postData != null) {
            con.setRequestMethod("POST");
            con.setDoOutput(true);
        }
        con.setConnectTimeout(5000);
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("User-Agent", GRADLE_USER_AGENT);
        if (postData != null) {
            byte[] rawPostData = postData.getBytes(StandardCharsets.UTF_8);
            con.setFixedLengthStreamingMode(rawPostData.length);
            con.connect();
            try (OutputStream postDataOutputStream = con.getOutputStream();) {
                postDataOutputStream.write(rawPostData);
            }
        }
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
