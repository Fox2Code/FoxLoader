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
package com.fox2code.foxloader.patching;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class PreLoaderMetaJarHash {
    private final MessageDigest digest;
    private final ByteArrayOutputStream baos;
    private final DataOutputStream dos;
    private byte[] cache;

    public PreLoaderMetaJarHash() {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.baos = new ByteArrayOutputStream();
        this.dos = new DataOutputStream(this.baos);
    }

    public void addString(String text) {
        if (this.cache != null)
            throw new IllegalStateException("Hash has been frozen");
        try {
            this.dos.writeUTF(text);
            this.dos.writeByte(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] makeHash() {
        if (this.cache != null) return this.cache;
        byte[] result = this.digest.digest(this.baos.toByteArray());
        if (result.length != 32) {
            throw new AssertionError(
                    "Result hash is not the result hash of a SHA-256 hash " +
                            "(got " + result.length + ", expected 32)");
        }
        return result;
    }

    public String getHash() {
        byte[] hash = makeHash();
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }

    public void freeze() {
        this.cache = this.makeHash();
    }
}
