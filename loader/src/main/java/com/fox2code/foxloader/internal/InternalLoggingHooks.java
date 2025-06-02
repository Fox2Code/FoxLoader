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
