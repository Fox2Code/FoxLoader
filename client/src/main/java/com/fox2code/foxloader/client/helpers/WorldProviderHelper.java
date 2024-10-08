package com.fox2code.foxloader.client.helpers;

import com.fox2code.foxloader.client.WorldProviderCustom;
import net.minecraft.src.game.level.WorldProvider;

import java.lang.reflect.InvocationTargetException;

public class WorldProviderHelper {
    public static void addWorldProvider(String name,WorldProviderCustom worldProvider) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        WorldProviderCustom.addprovider(name,worldProvider);
    }
}