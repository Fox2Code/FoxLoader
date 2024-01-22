package com.fox2code.foxloader.loader.lua;

import com.fox2code.foxloader.launcher.FoxLauncher;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.LuajavaLib;

import java.lang.ref.WeakReference;

public final class FoxLuajavaLib extends LuajavaLib {
    private static final int NEWINSTANCE	= 2;
    private static final int NEW			= 3;

    public Varargs invoke(Varargs args) {
        switch (opcode) {
            default: {
                return super.invoke(args);
            }
            case NEWINSTANCE:
            case NEW: {
                Varargs varargs = super.invoke(args);
                if (varargs instanceof LuaUserdata) {
                    Object userData = ((LuaUserdata) varargs).m_instance;
                    if (userData instanceof LuaObjectHolder) {
                        ((LuaObjectHolder)userData).foxLoader$setLuaObject(
                                new WeakReference<>((LuaValue) varargs));
                    }
                }
                return varargs;
            }
        }
    }

    @Override
    protected Class<?> classForName(String name) throws ClassNotFoundException {
        return FoxLauncher.getFoxClassLoader().loadClass(name);
    }
}
