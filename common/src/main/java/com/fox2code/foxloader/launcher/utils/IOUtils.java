package com.fox2code.foxloader.launcher.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
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
}
