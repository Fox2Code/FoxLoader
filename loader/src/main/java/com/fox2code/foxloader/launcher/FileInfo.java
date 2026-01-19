/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.utils.SourceUtil;
import com.fox2code.foxloader.utils.io.IOUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;

public class FileInfo {
    private static final byte[] emptyHash = new byte[32];
    public final File file;
    public final String jarPath;
    public final URL source;
    public final String sha256;
    public final String fileName;
    private final byte[] bSha256;
    final CodeSource codeSource;
    final Boolean isJavaArchive0;

    public FileInfo(File file) throws IOException {
        this(file, null);
    }

    public FileInfo(File file, String jarPath) throws IOException {
        file = file.getClass() != File.class ? new File(file.getPath()) : file;
        file = file.isAbsolute() ? file : file.getAbsoluteFile();
        this.file = file;
        this.jarPath = jarPath;
        if (jarPath != null) {
            this.source = new URL("jar:" + file.toURI().toURL() + "!/" + this.jarPath);
            this.bSha256 = IOUtils.sha256Of(this.source);
            this.sha256 = IOUtils.toHex(this.bSha256);
            this.fileName = this.jarPath.substring(this.jarPath.lastIndexOf('/') + 1);
        } else {
            this.source = file.toURI().toURL();
            this.bSha256 = file.isDirectory() ? emptyHash : IOUtils.sha256Of(file);
            this.sha256 = IOUtils.toHex(this.bSha256);
            this.fileName = file.getName();
        }
        this.codeSource = new CodeSourceWithFileInfo(this.source, this);
        Boolean isJavaArchive = null;
        try {
            isJavaArchive = this.isJavaArchive();
        } catch (Throwable ignored) {}
        this.isJavaArchive0 = isJavaArchive;
    }

    public FileInfo(DataInputStream dataInputStream) throws IOException {
        this.file = null;
        this.jarPath = readStringSafest(dataInputStream, null);
        this.source = null;
        this.bSha256 = new byte[32];
        dataInputStream.readFully(this.bSha256);
        this.sha256 = IOUtils.toHex(this.bSha256);
        this.fileName = readStringSafest(dataInputStream, null);
        this.codeSource = null;
        this.isJavaArchive0 = false;
    }

    public static String readStringSafest(DataInputStream dataInputStream, String fallback) throws IOException {
        int len = dataInputStream.readUnsignedByte();
        if (len == 0) return fallback;
        StringBuilder stringBuilder = new StringBuilder(len);
        while (len-->0) {
            stringBuilder.append(dataInputStream.readChar());
        }
        return stringBuilder.toString();
    }

    public final void emitSha256(byte[] output) {
        if (output.length != 32) {
            throw new IllegalArgumentException("Argument must be a 32 long byte array");
        }
        System.arraycopy(this.bSha256, 0, output, 0, 32);
    }

    public boolean isJavaArchive() {
        return this.fileName.endsWith(".jar");
    }

    public final boolean isRemote() {
        return this.file == null;
    }

    public final void assertLocalFile() {
        if (this.isRemote()) {
            throw new AssertionError("File info is not a local file info");
        }
    }

    public final URI toURI() {
        try {
            return this.source.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CodeSourceWithFileInfo extends CodeSource implements SourceUtil.CodeSourceFileProvider {
        private final FileInfo fileInfo;

        CodeSourceWithFileInfo(URL url, FileInfo fileInfo) {
            super(url, FoxClassLoader.NO_CodeSigners);
            this.fileInfo = fileInfo;
        }

        public FileInfo getFileInfo() {
            return this.fileInfo;
        }

        @Override
        public File getFile() {
            return this.fileInfo.file;
        }
    }
}
