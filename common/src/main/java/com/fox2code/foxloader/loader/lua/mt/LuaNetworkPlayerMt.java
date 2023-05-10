package com.fox2code.foxloader.loader.lua.mt;

import com.fox2code.foxloader.loader.lua.LuaVMHelper;
import com.fox2code.foxloader.network.NetworkPlayer;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class LuaNetworkPlayerMt {
    public static final HashMap<String, LuaValue> metaTable = new HashMap<>();

    static {
        metaTable.put("isOperator", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (arg instanceof LuaUserdata && ((LuaUserdata) arg).m_instance instanceof NetworkPlayer) {
                    NetworkPlayer networkPlayer = (NetworkPlayer) ((LuaUserdata) arg).m_instance;
                    return LuaVMHelper.booleanToLuaTransformer.apply(networkPlayer.isOperator());
                }
                return LuaValue.NIL;
            }
        });
        metaTable.put("getPlayerName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (arg instanceof LuaUserdata && ((LuaUserdata) arg).m_instance instanceof NetworkPlayer) {
                    NetworkPlayer networkPlayer = (NetworkPlayer) ((LuaUserdata) arg).m_instance;
                    return LuaString.valueOf(networkPlayer.getPlayerName());
                }
                return LuaValue.NIL;
            }
        });
        metaTable.put("displayChatMessage", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                if (arg1 instanceof LuaUserdata && ((LuaUserdata) arg1).m_instance instanceof NetworkPlayer) {
                    NetworkPlayer networkPlayer = (NetworkPlayer) ((LuaUserdata) arg1).m_instance;
                    networkPlayer.displayChatMessage(arg2.tojstring());
                }
                return LuaValue.NIL;
            }
        });
        metaTable.put("kick", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                if (arg1 instanceof LuaUserdata && ((LuaUserdata) arg1).m_instance instanceof NetworkPlayer) {
                    NetworkPlayer networkPlayer = (NetworkPlayer) ((LuaUserdata) arg1).m_instance;
                    networkPlayer.kick(arg2.tojstring());
                }
                return LuaValue.NIL;
            }
        });
    }
}
