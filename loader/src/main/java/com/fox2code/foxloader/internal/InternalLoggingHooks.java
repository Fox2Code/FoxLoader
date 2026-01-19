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

import com.fox2code.foxloader.launcher.FoxLauncher;
import net.minecraft.common.util.logging.LogAgent;
import net.minecraft.common.util.logging.LoggingFormatter;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InternalLoggingHooks {
    private static final boolean restoreVanillaLogging = Boolean.getBoolean("foxloader.restoreVanillaLogging");
    private static final LoggingFormatter FORMATTER = new LoggingFormatter();
    private static final ConsoleHandler CONSOLE_HANDLER = new ConsoleHandler();

    private InternalLoggingHooks() {}

    static {
        CONSOLE_HANDLER.setFormatter(FORMATTER);
    }

    public static void setupLogAgentFoxLoader(LogAgent logAgent) {
        Logger logger = logAgent.getLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(CONSOLE_HANDLER);
        if (FoxLauncher.isTestingMode()) return;
        FoxLauncher.installLoggerHelperOn(logger);
        if (logAgent.logFile == null || !restoreVanillaLogging) return;
        try {
            FileHandler fileHandler = new FileHandler(logAgent.logFile, true);
            fileHandler.setFormatter(FORMATTER);
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to log " + logAgent.loggerName + " to " + logAgent.logFile, e);
        }
    }
}
