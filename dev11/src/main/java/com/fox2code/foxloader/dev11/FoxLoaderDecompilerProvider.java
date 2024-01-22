package com.fox2code.foxloader.dev11;

import com.fox2code.foxloader.dev.IFoxLoaderDecompiler;
import com.fox2code.foxloader.dev.IFoxLoaderDecompilerProvider;

import java.io.File;

public class FoxLoaderDecompilerProvider implements IFoxLoaderDecompilerProvider {
    public FoxLoaderDecompilerProvider() {}

    @Override
    public IFoxLoaderDecompiler newDecompiler(File source, File destination, boolean client) {
        return new FoxLoaderDecompiler(source, destination, client);
    }
}
