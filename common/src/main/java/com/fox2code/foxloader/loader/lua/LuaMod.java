package com.fox2code.foxloader.loader.lua;

import com.fox2code.foxloader.loader.Mod;
import com.fox2code.foxloader.network.NetworkPlayer;
import org.luaj.vm2.*;

public final class LuaMod extends Mod {
    private static final LuaValue KEY_ON_PRE_INIT = LuaString.valueOf("onPreInit");
    private static final LuaValue KEY_ON_INIT = LuaString.valueOf("onInit");
    private static final LuaValue KEY_ON_POST_INIT = LuaString.valueOf("onPostInit");
    private static final LuaValue KEY_ON_TICK = LuaString.valueOf("onTick");
    private static final LuaValue KEY_NETWORK_PLAYER_JOINED = LuaString.valueOf("onNetworkPlayerJoined");
    private static final LuaValue KEY_NETWORK_PLAYER_DISCONNECTED = LuaString.valueOf("onNetworkPlayerDisconnected");
    private final LuaValue function;
    private final LuaTable mod;
    private LuaFunction onPreInit;
    private LuaFunction onInit;
    private LuaFunction onPostInit;
    private LuaFunction onTick;
    private LuaFunction onNetworkPlayerJoined;
    private LuaFunction onNetworkPlayerDisconnected;

    LuaMod(LuaValue function, LuaTable mod) {
        this.function = function;
        this.mod = mod;
    }

    @Override
    public void onPreInit() {
        function.call();
        this.updateHookFunctions();
        if (this.onPreInit != null) {
            this.onPreInit.call();
        }
    }

    @Override
    public void onInit() {
        this.updateHookFunctions();
        if (this.onInit != null) {
            this.onInit.call();
        }
    }

    @Override
    public void onPostInit() {
        this.updateHookFunctions();
        if (this.onPostInit != null) {
            this.onPostInit.call();
        }
        this.updateHookFunctions();
    }

    @Override
    public void onTick() {
        if (this.onTick != null) {
            this.onTick.call();
        }
    }

    @Override
    public void onNetworkPlayerJoined(NetworkPlayer networkPlayer) {
        if (this.onNetworkPlayerJoined != null) {
            this.onNetworkPlayerJoined.call(LuaVMHelper.luaDataOf(networkPlayer));
        }
    }

    @Override
    public boolean onNetworkPlayerDisconnected(NetworkPlayer networkPlayer, String kickMessage, boolean cancelled) {
        if (this.onNetworkPlayerDisconnected != null) {
            return this.onNetworkPlayerDisconnected.call(LuaVMHelper.luaDataOf(networkPlayer),
                    LuaVMHelper.luaDataOf(kickMessage), LuaVMHelper.luaDataOf(cancelled)).toboolean();
        }
        return false;
    }

    private void updateHookFunctions() {
        this.onPreInit = this.mod.rawget(KEY_ON_PRE_INIT).optfunction(null);
        this.onInit = this.mod.rawget(KEY_ON_INIT).optfunction(null);
        this.onPostInit = this.mod.rawget(KEY_ON_POST_INIT).optfunction(null);
        this.onTick = this.mod.rawget(KEY_ON_TICK).optfunction(null);
        this.onNetworkPlayerJoined = this.mod.rawget(KEY_NETWORK_PLAYER_JOINED).optfunction(null);
        this.onNetworkPlayerDisconnected = this.mod.rawget(KEY_NETWORK_PLAYER_DISCONNECTED).optfunction(null);
    }
}
