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
package com.fox2code.foxloader.loader.early;

import net.minecraft.common.ICoreAccess;
import net.minecraft.common.entity.Entity;
import net.minecraft.common.entity.player.PlayerInteractionHandler;
import net.minecraft.common.util.logging.LogAgent;

import java.io.File;
import java.util.List;

final class Testing implements ICoreAccess {
    static final LogAgent TESTING_LOG_AGENT = new LogAgent("TESTING", null);

    @Override
    public File getMinecraftDir() {
        throw new IllegalStateException("getMinecraftDir() is not available in testing mode");
    }

    @Override
    public PlayerInteractionHandler getPlayerInteractionHandler() {
        throw new IllegalStateException("getPlayerInteractionHandler() is not available in testing mode");
    }

    @Override
    public void tickSprint(int i) {
        throw new IllegalStateException("tickSprint() is not available in testing mode");
    }

    @Override
    public LogAgent getLogger() {
        return TESTING_LOG_AGENT;
    }

    @Override
    public void appendAllLoadedEntities(List<Entity> list) {
        throw new IllegalStateException("appendAllLoadedEntities() is not available in testing mode");
    }
}
