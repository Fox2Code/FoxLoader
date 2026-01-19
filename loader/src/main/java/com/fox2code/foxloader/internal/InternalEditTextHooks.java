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
package com.fox2code.foxloader.internal;

import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxloader.event.text.PlayerEditCuneiformBlockEvent;
import com.fox2code.foxloader.event.text.PlayerEditSignEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEditCuneiformBlock;
import net.minecraft.client.gui.GuiEditSign;
import net.minecraft.common.block.tileentity.TileEntityCuneiformBlock;
import net.minecraft.common.block.tileentity.TileEntitySign;
import net.minecraft.common.networking.Packet130UpdateSign;
import net.minecraft.common.networking.Packet133UpdateCuneiformBlock;
import net.minecraft.common.world.World;
import net.minecraft.server.networking.NetServerHandler;

public final class InternalEditTextHooks {
    private static final EventHolder<PlayerEditCuneiformBlockEvent> PLAYER_EDIT_CUNEIFORM_BLOCK_EVENT =
            EventHolder.getHolderFromEvent(PlayerEditCuneiformBlockEvent.class);
    private static final EventHolder<PlayerEditSignEvent> PLAYER_EDIT_SIGN_EVENT =
            EventHolder.getHolderFromEvent(PlayerEditSignEvent.class);

    private InternalEditTextHooks() {}

    public static void handleLocalSignEdit(GuiEditSign guiEditSign, TileEntitySign tileEntitySign) {
        World worldClient = Minecraft.theMinecraft.theWorld;
        if (worldClient == null || PLAYER_EDIT_SIGN_EVENT.isEmpty()) {
            System.arraycopy(tileEntitySign.signText, 0, guiEditSign.getPreviousSignText(), 0, 4);
            return;
        }
        PlayerEditSignEvent playerEditSignEvent = new PlayerEditSignEvent(worldClient,
                tileEntitySign.xCoord, tileEntitySign.yCoord, tileEntitySign.zCoord,
                Minecraft.theMinecraft.thePlayer, tileEntitySign.signText, guiEditSign.getPreviousSignText());
        PLAYER_EDIT_SIGN_EVENT.callEvent(playerEditSignEvent);
        if (playerEditSignEvent.isCancelled()) {
            System.arraycopy(guiEditSign.getPreviousSignText(), 0, tileEntitySign.signText, 0, 4);
        } else {
            if (tileEntitySign.signText != playerEditSignEvent.getSignLinesNew()) {
                System.arraycopy(playerEditSignEvent.getSignLinesNew(), 0, tileEntitySign.signText, 0, 4);
            }
            System.arraycopy(tileEntitySign.signText, 0, guiEditSign.getPreviousSignText(), 0, 4);
        }
    }

    public static void handleLocalCuneiformEdit(
            GuiEditCuneiformBlock guiEditCuneiformBlock,
            TileEntityCuneiformBlock tileEntityCuneiformBlock, boolean finished) {
        World worldClient = Minecraft.theMinecraft.theWorld;
        if (worldClient == null || PLAYER_EDIT_CUNEIFORM_BLOCK_EVENT.isEmpty()) {
            return;
        }
        PlayerEditCuneiformBlockEvent playerEditCuneiformBlockEvent =
                new PlayerEditCuneiformBlockEvent(worldClient, tileEntityCuneiformBlock.xCoord,
                tileEntityCuneiformBlock.yCoord, tileEntityCuneiformBlock.zCoord,
                Minecraft.theMinecraft.thePlayer, tileEntityCuneiformBlock.getText(),
                guiEditCuneiformBlock.getText(), finished);
        PLAYER_EDIT_CUNEIFORM_BLOCK_EVENT.callEvent(playerEditCuneiformBlockEvent);
        guiEditCuneiformBlock.setText(playerEditCuneiformBlockEvent.isCancelled() ?
                playerEditCuneiformBlockEvent.getOldBlockText() :
                playerEditCuneiformBlockEvent.getNewBlockText());
    }

    public static boolean handleRemoteSignEdit(
            NetServerHandler netServerHandler,
            Packet130UpdateSign updateSignPacket, TileEntitySign tileEntitySign) {
        if (PLAYER_EDIT_SIGN_EVENT.isEmpty()) {
            return false;
        }
        PlayerEditSignEvent playerEditSignEvent = new PlayerEditSignEvent(netServerHandler.getWorld(),
                updateSignPacket.xPosition, updateSignPacket.yPosition, updateSignPacket.zPosition,
                netServerHandler.getEntityPlayer(), tileEntitySign.signText, updateSignPacket.signLines);
        PLAYER_EDIT_SIGN_EVENT.callEvent(playerEditSignEvent);
        if (playerEditSignEvent.isCancelled()) {
            netServerHandler.sendPacket(tileEntitySign.getDescriptionPacket());
            return true;
        }
        updateSignPacket.signLines = playerEditSignEvent.getSignLinesNew();
        return false;
    }

    public static boolean handleRemoteCuneiformEdit(
            NetServerHandler netServerHandler,
            Packet133UpdateCuneiformBlock updateSignPacket, TileEntityCuneiformBlock tileEntityCuneiform) {
        if (PLAYER_EDIT_CUNEIFORM_BLOCK_EVENT.isEmpty()) {
            return false;
        }
        PlayerEditCuneiformBlockEvent playerEditCuneiformBlockEvent = new PlayerEditCuneiformBlockEvent(netServerHandler.getWorld(),
                updateSignPacket.xPosition, updateSignPacket.yPosition, updateSignPacket.zPosition,
                netServerHandler.getEntityPlayer(), tileEntityCuneiform.getText(), updateSignPacket.text, updateSignPacket.finished);
        PLAYER_EDIT_CUNEIFORM_BLOCK_EVENT.callEvent(playerEditCuneiformBlockEvent);
        if (playerEditCuneiformBlockEvent.isCancelled()) {
            return true;
        }
        updateSignPacket.text = playerEditCuneiformBlockEvent.getNewBlockText();
        return false;
    }
}
