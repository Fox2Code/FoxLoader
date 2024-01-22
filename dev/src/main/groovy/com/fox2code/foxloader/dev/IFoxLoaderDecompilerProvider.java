package com.fox2code.foxloader.dev;

import java.io.File;

public interface IFoxLoaderDecompilerProvider {
    IFoxLoaderDecompiler newDecompiler(File source, File destination, boolean client);
}
