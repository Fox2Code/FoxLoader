package com.fox2code.foxloader.dependencies;

import com.fox2code.foxloader.launcher.FileInfo;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

public final class DependencyFileInfo extends FileInfo {
    private final DependencyHelper.Dependency dependency;

    public DependencyFileInfo(File file, DependencyHelper.Dependency dependency) throws IOException {
        super(file);
        this.dependency = dependency;
    }

    public DependencyFileInfo(DataInputStream dataInputStream) throws IOException {
        super(dataInputStream);
        this.dependency = new DependencyHelper.Dependency(
                FileInfo.readStringSafest(dataInputStream, null), FileInfo.readStringSafest(dataInputStream, null),
                FileInfo.readStringSafest(dataInputStream, null), FileInfo.readStringSafest(dataInputStream, null),
                FileInfo.readStringSafest(dataInputStream, null), dataInputStream.readUnsignedShort());
    }

    public DependencyHelper.Dependency getDependency() {
        return this.dependency;
    }

    @Override
    public boolean isJavaArchive() {
        return true;
    }
}
