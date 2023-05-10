package com.fox2code.foxloader.loader.lua;

import com.fox2code.foxloader.launcher.FoxLauncher;
import org.luaj.vm2.lib.jse.LuajavaLib;

public final class FoxLuajavaLib extends LuajavaLib {
    @Override
    protected Class<?> classForName(String name) throws ClassNotFoundException {
        return FoxLauncher.getFoxClassLoader().loadClass(name);
    }
}
