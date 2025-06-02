package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.launcher.FoxLauncher;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

final class FoxLoaderSparkClassSourceLookup extends ClassSourceLookup.ByCodeSource {
    public String identify(MethodCall methodCall) throws Exception {
        String className = methodCall.getClassName();
        String methodName = methodCall.getMethodName();
        String methodDesc = methodCall.getMethodDescriptor();
        if (!className.equals("native") && !methodName.equals("<init>") && !methodName.equals("<clinit>")) {
            Class<?> clazz = this.classForName(className);
            if (clazz == null) {
                return null;
            } else {
                Class<?>[] params = this.getParameterTypesForMethodDesc(methodDesc);
                if (params == null) {
                    return null;
                } else {
                    Method reflectMethod = clazz.getDeclaredMethod(methodName, params);
                    MixinMerged mixinMarker = reflectMethod.getDeclaredAnnotation(MixinMerged.class);
                    return mixinMarker == null ? null : modIdFromMixinClass(mixinMarker.mixin());
                }
            }
        } else {
            return null;
        }
    }

    private static String modIdFromMixinClass(String mixinClassName) {
        for(Config config : Mixins.getConfigs()) {
            IMixinConfig mixinConfig = config.getConfig();
            if (mixinClassName.startsWith(mixinConfig.getMixinPackage())) {
                return FabricUtil.getModId(mixinConfig);
            }
        }

        return null;
    }

    private Class<?>[] getParameterTypesForMethodDesc(String methodDesc) {
        Type methodType = Type.getMethodType(methodDesc);
        Class<?>[] params = new Class[methodType.getArgumentTypes().length];
        Type[] argumentTypes = methodType.getArgumentTypes();
        int i = 0;

        for(int argumentTypesLength = argumentTypes.length; i < argumentTypesLength; ++i) {
            Type argumentType = argumentTypes[i];
            if ((params[i] = this.getClassFromType(argumentType)) == null) {
                return null;
            }
        }

        return params;
    }

    private Class<?> getClassFromType(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return Void.TYPE;
            case Type.BOOLEAN:
                return Boolean.TYPE;
            case Type.CHAR:
                return Character.TYPE;
            case Type.BYTE:
                return Byte.TYPE;
            case Type.SHORT:
                return Short.TYPE;
            case Type.INT:
                return Integer.TYPE;
            case Type.FLOAT:
                return Float.TYPE;
            case Type.LONG:
                return Long.TYPE;
            case Type.DOUBLE:
                return Double.TYPE;
            case Type.ARRAY:
                Class<?> classFromType = this.getClassFromType(type.getElementType());
                Class<?> result = classFromType;
                if (classFromType != null) {
                    for (int i = 0; i < type.getDimensions(); ++i) {
                        result = Array.newInstance(result, 0).getClass();
                    }
                }
                return result;
            case Type.OBJECT:
                return this.classForName(type.getClassName());
            default:
                return null;
        }
    }

    private Class<?> classForName(String className) {
        try {
            return FoxLauncher.getFoxClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
