/*
 * MIT License
 * 
 * Copyright (c) 2023-2026 Fox2Code
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
package com.fox2code.foxloader.server;

import net.minecraft.common.entity.player.EntityPlayer;
import net.minecraft.common.networking.NetHandler;
import net.minecraft.common.networking.NetworkManager;
import net.minecraft.server.entity.player.EntityPlayerMP;
import net.minecraft.server.networking.NetLoginHandler;
import net.minecraft.server.networking.NetServerHandler;

public final class NetworkKickHelper {
    public static void kickPlayer(EntityPlayer entityPlayer, String message) {
        if (entityPlayer instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entityPlayer).playerNetServerHandler.kickPlayer(message);
        }
    }

    public static void kickPlayer(NetworkManager networkManager, String message) {
        NetHandler netHandler = networkManager.getNetHandler();
        if (netHandler instanceof NetLoginHandler) {
            ((NetLoginHandler) netHandler).kickUser(message);
        } else if (netHandler instanceof NetServerHandler) {
            ((NetServerHandler) netHandler).kickPlayer(message);
        }
    }
}
