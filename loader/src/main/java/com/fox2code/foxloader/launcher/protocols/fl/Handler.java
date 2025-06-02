package com.fox2code.foxloader.launcher.protocols.fl;

import com.fox2code.foxloader.launcher.FileInfo;
import com.fox2code.foxloader.launcher.FoxClassLoader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        FileInfo fileInfo = FoxClassLoader.FoxLoaderURLStreamHandler.getFileInfoForJarInJarPath(u.getPath());
        if (fileInfo == null) {
            throw new IOException("");
        }
        assert fileInfo.source != null;
        return fileInfo.source.openConnection();
    }
}
