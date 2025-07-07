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
import com.fox2code.foxloader.event.movement.PlayerJumpingEvent;
import com.fox2code.foxloader.event.movement.PlayerSneakingEvent;
import net.minecraft.common.entity.player.EntityPlayer;

public final class InternalMovementHooks {
    private static final EventHolder<PlayerJumpingEvent> PLAYER_JUMPING_EVENT =
            EventHolder.getHolderFromEvent(PlayerJumpingEvent.class);
    private static final EventHolder<PlayerSneakingEvent> PLAYER_SNEAKING_EVENT =
            EventHolder.getHolderFromEvent(PlayerSneakingEvent.class);

    private InternalMovementHooks() {}

    public static void onPlayerLivingUpdated(EntityPlayer entityPlayer) {
        boolean isJumping = entityPlayer.isJumping && !entityPlayer.isDead;
        boolean isSneaking = entityPlayer.isSneaking() && !entityPlayer.isDead;
        if (isJumping && !entityPlayer.flWasJumping && !PLAYER_JUMPING_EVENT.isEmpty()) {
            PLAYER_JUMPING_EVENT.callEvent(new PlayerJumpingEvent(entityPlayer));
        }
        if (isSneaking && !entityPlayer.flWasSneaking && !PLAYER_SNEAKING_EVENT.isEmpty()) {
            PLAYER_SNEAKING_EVENT.callEvent(new PlayerSneakingEvent(entityPlayer));
        }
        entityPlayer.flWasJumping = isJumping;
        entityPlayer.flWasSneaking = isSneaking;
    }
}
