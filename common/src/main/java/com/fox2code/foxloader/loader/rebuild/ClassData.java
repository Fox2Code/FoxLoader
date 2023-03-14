package com.fox2code.foxloader.loader.rebuild;

public abstract class ClassData {
    public abstract String getName();
    public abstract ClassData getSuperclass();
    public abstract ClassData[] getInterfaces();
    public abstract boolean isAssignableFrom(ClassData clData);
    public abstract boolean isInterface();
    public abstract boolean isFinal();
    public abstract boolean isPublic();
    public abstract boolean isCustom();
}
