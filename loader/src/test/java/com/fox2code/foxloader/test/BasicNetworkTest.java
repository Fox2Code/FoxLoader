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
package com.fox2code.foxloader.test;

import com.fox2code.foxloader.dependencies.DependencyHelper;
import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.loader.packet.ClientHello;
import com.fox2code.foxloader.utils.SourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;

public class BasicNetworkTest {
    @Test
    public void testFileInfoEncodingMatch() throws IOException {
        byte[] hashCache = new byte[32];
        FileInfo fileInfoLocal = new FileInfo(SourceUtil.getSourceFile(DependencyHelper.class));
        Assertions.assertFalse(fileInfoLocal.isRemote());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ClientHello.flWriteFileInfo(new DataOutputStream(byteArrayOutputStream), hashCache, fileInfoLocal);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        FileInfo parsedFileInfo = ClientHello.flReadFileInfo(new DataInputStream(byteArrayInputStream));
        Assertions.assertTrue(parsedFileInfo.isRemote());
        Assertions.assertEquals(0, byteArrayInputStream.available());
        Assertions.assertEquals(fileInfoLocal.sha256, parsedFileInfo.sha256);
    }
}
