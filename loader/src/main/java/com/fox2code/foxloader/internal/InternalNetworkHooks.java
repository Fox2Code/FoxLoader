/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.network.PlayerConnectEvent;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.loader.packet.LoaderNetworkManager;
import com.fox2code.foxloader.loader.packet.ServerHello;
import com.fox2code.foxloader.registry.GameRegistry;
import net.minecraft.common.networking.NetworkManager;
import net.minecraft.server.networking.NetLoginHandler;

import java.util.Objects;
import java.util.logging.Level;

public final class InternalNetworkHooks {
    private static final EventHolder<PlayerConnectEvent> PLAYER_CONNECT_EVENT =
            EventHolder.getHolderFromEvent(PlayerConnectEvent.class);
    private static final String NO_FOXLOADER_DENY_JOIN = "You must use FoxLoader 2.0 to join this server";
    private static boolean debugNetworkErrors = true;

    private InternalNetworkHooks() {}

    public static void onNetworkError(NetworkManager networkManager, Exception e) {
        Objects.requireNonNull(networkManager, "NetworkManager");
        if (InternalNetworkHooks.debugNetworkErrors) {
            ModLoaderInit.getModLoaderLogger().log(Level.SEVERE,
                    "Network error from " + networkManager.getRemoteAddress(), e);
        }
    }

    public static void setDebugNetworkErrors(boolean debugNetworkErrors) {
        InternalNetworkHooks.debugNetworkErrors = debugNetworkErrors;
    }

    public static boolean sendPlayerConnectEvent(NetworkManager networkManager, String username) {
        NetLoginHandler netLoginHandler = ((NetLoginHandler) networkManager.getNetHandler());
        if (netLoginHandler.finishedProcessing) return true;
        final boolean incompatibleClient = GameRegistry.Internal.isIncompatibleClient(networkManager);
        if (PLAYER_CONNECT_EVENT.isEmpty()) {
            if (incompatibleClient) {
                netLoginHandler.kickUser(NO_FOXLOADER_DENY_JOIN);
                return true;
            }
            GameRegistry.Internal.sendRegistryData(networkManager);
            return false;
        }
        PlayerConnectEvent playerConnectEvent =
                new PlayerConnectEvent(networkManager, username, incompatibleClient);
        PLAYER_CONNECT_EVENT.callEvent(playerConnectEvent);
        // If finishedProcessing only after calling the event, cancel was not used properly.
        if (netLoginHandler.finishedProcessing) return true;
        if (incompatibleClient && !playerConnectEvent.isCancelled()) {
            playerConnectEvent.cancel(NO_FOXLOADER_DENY_JOIN);
        }
        if (playerConnectEvent.isCancelled()) {
            netLoginHandler.kickUser(playerConnectEvent.getCancelMessage());
            return true;
        }
        GameRegistry.Internal.sendRegistryData(networkManager);
        return false;
    }

    public static void sendServerHello(NetworkManager networkManager) {
        LoaderNetworkManager.sendServerPacketData(networkManager, new ServerHello());
    }
}
