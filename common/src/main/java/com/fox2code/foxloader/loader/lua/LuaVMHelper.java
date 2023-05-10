package com.fox2code.foxloader.loader.lua;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.lua.mt.LuaNetworkPlayerMt;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.GameRegistry;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.function.Function;

public class LuaVMHelper {
    public static final Function<LuaValue, Boolean> luaToBooleanTransformer = v ->
            v instanceof LuaUserdata ? (Boolean) ((LuaUserdata) v).m_instance : v.toboolean();
    public static final Function<LuaValue, Byte> luaToByteTransformer = v ->
            v instanceof LuaUserdata ? (Byte) ((LuaUserdata) v).m_instance : v.tobyte();
    public static final Function<LuaValue, Short> luaToShortTransformer = v ->
            v instanceof LuaUserdata ? (Byte) ((LuaUserdata) v).m_instance : v.toshort();
    public static final Function<LuaValue, Integer> luaToIntTransformer = v ->
            v instanceof LuaUserdata ? (Integer) ((LuaUserdata) v).m_instance : v.toint();
    public static final Function<LuaValue, Float> luaToFloatTransformer = v ->
            v instanceof LuaUserdata ? (Integer) ((LuaUserdata) v).m_instance : v.tofloat();
    public static final Function<LuaValue, Long> luaToLongTransformer = v ->
            v instanceof LuaUserdata ? (Long) ((LuaUserdata) v).m_instance : v.tolong();
    public static final Function<LuaValue, Double> luaToDoubleTransformer = v ->
            v instanceof LuaUserdata ? (Double) ((LuaUserdata) v).m_instance : v.todouble();
    public static final Function<LuaValue, String> luaToStringTransformer = v ->
            v instanceof LuaUserdata ? (String) ((LuaUserdata) v).m_instance : v.tojstring();
    private static final IdentityHashMap<Class<?>, Function<LuaValue, ?>>
            classLuaToDefaultTransformer = new IdentityHashMap<>();
    private static final Function<Class<?>, Function<LuaValue, ?>>
            classLuaToDefaultTransformerFunction = c -> v ->
            v == null ? LuaValue.NIL : c.cast(((LuaUserdata) v).m_instance);
    public static final Function<?, LuaValue> defaultToLuaTransformer = v ->
            v == null ? LuaValue.NIL : v instanceof String ? LuaString.valueOf((String) v) :
                    LuaValue.userdataOf(v);
    public static final Function<Void, LuaValue> voidToLuaTransformer = v -> LuaValue.NIL;
    public static final Function<Boolean, LuaValue> booleanToLuaTransformer = v ->
            v == null ? LuaValue.NIL : v ? LuaValue.TRUE : LuaValue.FALSE;
    public static final Function<Number, LuaValue> numberToLuaTransformer = v ->
            v == null ? LuaValue.NIL : LuaDouble.valueOf(v.doubleValue());
    private static final LuaString KEY_GET_MOD_CONTAINER = LuaString.valueOf("getModContainer");
    private static final LuaString KEY_PRINT = LuaString.valueOf("print");
    private static final LuaString KEY_MOD = LuaString.valueOf("mod");
    private static final LuaTable networkPlayerMt = new LuaTable();
    private static Globals globals;

    public static void initialize() {
        if (globals == null) {
            initializeOnThread();
        }
    }

    public static LuaMod loadMod(final ModContainer modContainer) throws IOException {
        if (globals == null) {
            initialize();
        }
        LuaTable environment = new LuaTable();
        LuaValue[] keys = globals.keys();
        for (LuaValue key : keys) {
            LuaValue value;
            if (key.isstring() &&
                    key.tojstring().equals("_G")) {
                value = environment;
            } else {
                value = globals.get(key);
            }
            environment.rawset(key, value);
            environment.setmetatable(globals.getmetatable());
        }
        LuaTable mod = new LuaTable();
        environment.set(KEY_MOD, mod);
        environment.set(KEY_PRINT, new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                modContainer.logger.info(arg.tojstring());
                return LuaValue.NIL;
            }
        });
        mod.set(KEY_GET_MOD_CONTAINER, new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return luaDataOf(modContainer);
            }
        });
        LuaValue luaValue = LuaVMHelper.globals.load(
                Files.newInputStream(modContainer.file.toPath()),
                modContainer.id, "t", environment);
        return new LuaMod(luaValue, mod);
    }

    private static void initializeOnThread() {
        Globals globals = new Globals();
        LuaVMHelper.globals = globals;
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        // globals.load(new CoroutineLib()); // Don't allow CoroutineLib usage
        globals.load(new JseMathLib());
        globals.load(new JseIoLib());
        globals.load(new JseOsLib());
        globals.load(new FoxLuajavaLib());
        globals.load(new DebugLib());
        globals.load(new LuaGameRegistryLib());
        LoadState.install(globals);
        LuaC.install(globals);
        networkPlayerMt.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                if (arg1 instanceof LuaUserdata &&
                        ((LuaUserdata) arg1).m_instance instanceof NetworkPlayer) {
                    return LuaNetworkPlayerMt.metaTable.getOrDefault(arg2.tojstring(), LuaValue.NIL);
                }
                return LuaValue.NIL;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> void fillLuaTableWithInstanceMethods(LuaTable luaTable, Class<T> type,final T instance) {
        for (Method method : GameRegistry.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) &&
                    Modifier.isStatic(method.getModifiers()) == (instance == null)) {
                LuaInterop luaInterop = method.getAnnotation(LuaInterop.class);
                if (luaInterop == null) continue;
                final Class<?>[] parameters = method.getParameterTypes();
                String[] defaults = luaInterop.value();
                final Object[] pDefaults;
                if (defaults.length == 0) {
                    pDefaults = null;
                } else if (defaults.length != parameters.length){
                    throw new IllegalArgumentException("Arg mismatch with default length");
                } else {
                    pDefaults = new Object[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        if (parameters[i] == String.class) {
                            pDefaults[i] = defaults[i];
                        } else if (parameters[i] == int.class) {
                            String pDefault = defaults[i];
                            if (pDefault.startsWith("0x")) {
                                pDefaults[i] = Integer.parseInt(
                                        defaults[i].substring(2));
                            } else {
                                pDefaults[i] = Integer.parseInt(defaults[i]);
                            }
                        }
                    }
                }
                final Function<LuaValue, ?>[] pTransformers = new Function[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    pTransformers[i] = getToNativeTransformer(type);
                }
                final Function<Object, LuaValue> rTransformer = (Function<Object, LuaValue>)
                        getToLuaTransformer(method.getReturnType());
                final Method methodFinal = method;
                luaTable.set(method.getName(), new VarArgFunction() {
                    private final Object[] argBuffer = new Object[pTransformers.length];

                    @Override
                    public Varargs invoke(Varargs args) {
                        Arrays.fill(argBuffer, null);
                        final int nArgs = args.narg();
                        if (pDefaults == null) {
                            if (nArgs != pTransformers.length) {
                                throw new LuaError("Needed " + pTransformers.length + " args but got " + nArgs);
                            }
                            for (int i = 0; i < pTransformers.length;i++) {
                                argBuffer[i] = pTransformers[i].apply(args.arg(i + 1));
                            }
                        } else {
                            if (nArgs > pTransformers.length) {
                                throw new LuaError("Needed " + pTransformers.length + " args but got " + nArgs);
                            }

                            for (int i = 0; i < nArgs;i++) {
                                argBuffer[i] = pTransformers[i].apply(args.arg(i + 1));
                            }
                            if (pTransformers.length - nArgs > 0) {
                                System.arraycopy(pDefaults, nArgs, argBuffer, nArgs, pTransformers.length - nArgs);
                            }
                        }

                        Object returnValue;
                        try {
                            returnValue = methodFinal.invoke(instance, this.argBuffer);
                        } catch (Exception e) {
                            throw new LuaError(e);
                        }
                        Arrays.fill(argBuffer, null);
                        return rTransformer.apply(returnValue);
                    }
                });
            }
        }
    }

    public static Function<LuaValue, ?> getToNativeTransformer(Class<?> cls) {
        switch (cls.getName()) {
            default:
                return classLuaToDefaultTransformer.computeIfAbsent(
                        cls, classLuaToDefaultTransformerFunction);
            case "boolean":
                return luaToBooleanTransformer;
            case "byte":
                return luaToByteTransformer;
            case "short":
                return luaToShortTransformer;
            case "int":
                return luaToIntTransformer;
            case "float":
                return luaToFloatTransformer;
            case "long":
                return luaToLongTransformer;
            case "double":
                return luaToDoubleTransformer;
            case "java.lang.String":
                return luaToStringTransformer;
        }
    }

    public static Function<?, LuaValue> getToLuaTransformer(Class<?> cls) {
        switch (cls.getName()) {
            default:
                return defaultToLuaTransformer;
            case "void":
                return voidToLuaTransformer;
            case "boolean":
                return booleanToLuaTransformer;
            case "byte":
            case "short":
            case "int":
            case "float":
            case "long":
            case "double":
                return numberToLuaTransformer;
        }
    }

    public static LuaValue luaDataOf(Object o) {
        if (o == null) {
            return LuaValue.NIL;
        }
        if (o instanceof LuaValue) {
            return (LuaValue) o;
        }
        if (o instanceof Number) {
            return LuaDouble.valueOf(((Number) o).doubleValue());
        }
        if (o instanceof String) {
            return LuaString.valueOf(((String) o));
        }
        if (o instanceof Boolean) {
            return booleanToLuaTransformer.apply((Boolean) o);
        }
        if (o instanceof NetworkPlayer) {
            return LuaValue.userdataOf(o, networkPlayerMt);
        }
        return LuaValue.userdataOf(o);
    }

    public static LuaValue[] luaDataOf(Object... o) {
        LuaValue[] luaValues = new LuaValue[o.length];
        for (int i = 0; i < o.length; i++) {
            luaValues[i] = luaDataOf(o[i]);
        }
        return luaValues;
    }

    public static Varargs luaVarargsOf(Object... o) {
        return LuaValue.varargsOf(luaDataOf(o));
    }
}
