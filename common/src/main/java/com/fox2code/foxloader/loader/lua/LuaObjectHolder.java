package com.fox2code.foxloader.loader.lua;

import org.luaj.vm2.LuaValue;

import java.lang.ref.WeakReference;

public interface LuaObjectHolder {
    WeakReference<LuaValue> foxLoader$getLuaObject();

    void foxLoader$setLuaObject(WeakReference<LuaValue> foxLoader$LuaObject);
}
