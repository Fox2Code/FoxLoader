package com.fox2code.foxloader.loader.lua;

import com.fox2code.foxloader.network.ChatColors;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import com.fox2code.foxloader.registry.GameRegistry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

final class LuaGameRegistryLib extends TwoArgFunction {
    private final GameRegistry gameRegistry = Objects.requireNonNull(
            GameRegistry.getInstance(), "gameRegistry");

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable gameRegistry = new LuaTable();
        LuaVMHelper.fillLuaTableWithInstanceMethods(
                gameRegistry, GameRegistry.class, this.gameRegistry);
        gameRegistry.set("registerCommand", new RegisterCommand(false));
        gameRegistry.set("registerClientCommand", new RegisterCommand(true));
        env.set("gameRegistry", gameRegistry);
        LuaTable chatColors = new LuaTable();
        for (Field field : ChatColors.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    field.getType() == String.class) {
                try {
                    chatColors.set(field.getName(),
                            LuaString.valueOf((String) field.get(null)));
                } catch (Exception ignored) {}
            }
        }
        env.set("chatColors", chatColors);
        return env;
    }

    private static class RegisterCommand extends VarArgFunction {
        public RegisterCommand(boolean client) {
            this.opcode = client ? 1 : 0;
        }

        @Override
        public Varargs invoke(Varargs args) {
            final LuaFunction main = args.checkfunction(1);
            String name = args.checkjstring(2);
            boolean opOnly = args.optboolean(3, true);
            boolean isHidden = args.optboolean(4, false);

            CommandCompat commandCompat = new CommandCompat(
                    name, opOnly, isHidden, CommandCompat.NO_ALIASES) {
                @Override
                public void onExecute(String[] args, NetworkPlayer commandExecutor) {
                    LuaString[] luaArgs = new LuaString[args.length];
                    for (int i = 0; i < luaArgs.length; i++) {
                        luaArgs[i] = LuaString.valueOf(args[i]);
                    }
                    main.call(LuaTable.tableOf(null, luaArgs),
                            LuaVMHelper.luaDataOf(commandExecutor));
                }
            };
            if (this.opcode == 0) {
                CommandCompat.registerCommand(commandCompat);
            } else {
                CommandCompat.registerClientCommand(commandCompat);
            }
            return LuaVMHelper.luaDataOf(commandCompat);
        }

    }
}
