package com.fox2code.foxloader.launcher;

import com.fox2code.foxloader.utils.CustomLogLevel;
import com.fox2code.foxloader.utils.StackTraceStringifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.logging.*;

final class LoggerHelper {
    private static final String formatDefault = "[%1$tT] [%2$-7s] %3$s";
    private static final String formatClient = "[%1$tT] [CLIENT] [%2$-7s] %3$s";
    private static final String formatServer = "[%1$tT] [SERVER] [%2$-7s] %3$s";
    static final boolean devEnvironment = FoxLauncher.DEVELOPING_FOXLOADER || FoxLauncher.DEV_MODE;
    static final boolean consoleSupportColor = devEnvironment ||
            Boolean.getBoolean("foxloader.console-support-color") || System.console() != null;
    private static final boolean disableLoggerHelper =
            Boolean.getBoolean("foxloader.disable-logger-helper");
    private static final boolean loggerDistinguishSide =
            Boolean.getBoolean("foxloader.logger-distinguish-side");
    private static final String format = loggerDistinguishSide ?
            FoxLauncher.isServer() ? formatServer : formatClient : formatDefault;
    private static final FoxLoaderLogFormatter simpleFormatter = new FoxLoaderLogFormatter();
    private static final WrappedSystemOutConsoleHandler wrappedConsoleHandler = new WrappedSystemOutConsoleHandler();
    private static SystemOutConsoleHandler systemOutConsoleHandler;
    private static DirectFileHandler directFileHandler;
    private static FoxLoaderLogPrintStream flOut, flErr;
    private static PrintStream currentSystemOut;

    static boolean install(File logFile) {
        if (disableLoggerHelper) {
            System.out.println("The LoggerHelper has been disabled, things aren't gonna look pretty");
            return true;
        }
        if (System.out.getClass() != PrintStream.class) {
            System.out.println("System out has been modified, skipping install.");
            try (PrintStream printStream = new PrintStream(Files.newOutputStream(logFile.toPath()))){
                new CantInstallLoggerHelperException( // Help with debugging
                        "Failed to install LoggerHelper cause the current output class is " +
                                System.out.getClass().getName()).printStackTrace(printStream);
            } catch (IOException ignored) {}
            // If System.out already has been replaced just ignore the replacement.
            return false;
        }
        currentSystemOut = System.out;
        boolean installed = false;
        final Logger rootLogger = LogManager.getLogManager().getLogger("");
        final DirectFileHandler directFileHandler;
        try {
            directFileHandler = new DirectFileHandler(logFile);
            rootLogger.addHandler(directFileHandler);
        } catch (Exception ignored) {
            return false;
        }
        LoggerHelper.systemOutConsoleHandler = new SystemOutConsoleHandler();
        LoggerHelper.directFileHandler = directFileHandler;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            directFileHandler.flush();
            wrappedConsoleHandler.flush();
        }, "LoggerHelper exit flush shutdown hook"));
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                installed = true;
                rootLogger.removeHandler(handler);
                rootLogger.addHandler(wrappedConsoleHandler);
            } else {
                handler.setFormatter(LoggerHelper.simpleFormatter);
            }
        }
        if (installed) {
            final PrintStream out = System.out;
            out.flush(); // <- Make sure buffer is flushed
            System.setOut(flOut = new FoxLoaderLogPrintStream(out, rootLogger, CustomLogLevel.STDOUT, false));
            System.setErr(flErr = new FoxLoaderLogPrintStream(out, rootLogger, CustomLogLevel.STDERR, true));
        }
        return installed;
    }

    static void installOn(Logger logger) {
        System.out.println("Installing on logger: " + logger.getName());
        Handler[] handlers = logger.getHandlers();
        boolean hasDirectFileHandler = false;
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                if (handler != wrappedConsoleHandler) {
                    logger.removeHandler(handler);
                    logger.addHandler(wrappedConsoleHandler);
                    System.out.println("Replacing console logger: " + logger.getName());
                }
            } else if (handler == directFileHandler) {
                hasDirectFileHandler = true;
            }
        }
        if (!logger.getUseParentHandlers() && !hasDirectFileHandler) {
            logger.addHandler(directFileHandler);
        }
    }

    static void onJAnsiInstalled() {
        PrintStream out = System.out;
        if (out == flOut) return;
        currentSystemOut = out;
        out.flush(); // <- Make sure buffer is flushed
        System.setOut(flOut = flOut.rebuild());
        System.setErr(flErr = flErr.rebuild());
        wrappedConsoleHandler.flush();
        systemOutConsoleHandler = new SystemOutConsoleHandler();
    }

    @SuppressWarnings({"UnnecessaryCallToStringValueOf", "StringOperationCanBeSimplified"})
    private static final class FoxLoaderLogPrintStream extends PrintStream {
        private final Logger rootLogger;
        private final Level level;
        private final boolean doSkips;
        private boolean skip;

        public FoxLoaderLogPrintStream(@NotNull OutputStream out, Logger rootLogger, Level level, boolean doSkips) {
            super(out, true);
            this.rootLogger = rootLogger;
            this.level = level;
            this.doSkips = doSkips;
        }

        @Override
        public void println() {
            this.println("");
        }

        @Override
        public void print(int i) {
            this.print(String.valueOf(i));
        }

        @Override
        public void println(@Nullable Object x) {
            this.println(String.valueOf(x));
        }

        @Override
        public void println(@Nullable String line) {
            if (line == null) line = "null";
            if (doSkips && line.startsWith( // Normal on linux!
                    "java.io.IOException: Cannot run program \"sensible-browser\"")) {
                skip = true;
            } else if (skip) {
                if (!line.startsWith("\t") && !line.startsWith("    ")
                        && !line.startsWith("Caused by:")) {
                    skip = false;
                }
            }
            if (!skip) {
                rootLogger.log(level, line);
            }
        }

        @Override
        public void flush() {
            super.flush();
            directFileHandler.flush();
        }

        FoxLoaderLogPrintStream rebuild() {
            if (this.out == currentSystemOut) return this;
            return new FoxLoaderLogPrintStream(currentSystemOut,
                    this.rootLogger, this.level, this.doSkips);
        }
    }

    private static class FoxLoaderLogFormatter extends SimpleFormatter {
        private final Date date = new Date();

        @Override
        public synchronized String format(LogRecord lr) {
            String message = lr.getMessage();
            String sessionToken = FoxLauncher.initialSessionId;
            if (sessionToken != null && sessionToken.length() > 4) {
                message = message.replace(sessionToken, "<session token>");
            }
            String loggerName = lr.getLoggerName();
            if (loggerName != null && !loggerName.isEmpty() && !"global".equals(loggerName)) {
                message = "[" + loggerName + "] " + message;
            }

            Throwable throwable = lr.getThrown();
            if (throwable != null) {
                message += "\n" + StackTraceStringifier.stringifyStackTrace(throwable);
            }
            date.setTime(lr.getMillis());
            return String.format(format, date,
                    lr.getLevel().getLocalizedName(),
                    message
            ).trim() + '\n';
        }
    }

    private static class FoxLoaderConsoleLogFormatter extends FoxLoaderLogFormatter {
        public static final String RESET = "\033[0m";
        public static final String RED = "\033[0;31m";
        public static final String GREEN = "\033[0;32m";
        public static final String YELLOW = "\033[0;33m";
        public static final String BLUE = "\033[0;34m";

        @Override
        public synchronized String format(LogRecord lr) {
            String text = super.format(lr);
            if (!consoleSupportColor) return text;
            String color;
            switch (lr.getLevel().intValue()) {
                case 500: // FINE
                    color = GREEN;
                    break;
                case 700: // CONFIG
                    color = BLUE;
                    break;
                case 900: // WARNING
                    color = YELLOW;
                    break;
                case 1000: // SEVERE
                    color = RED;
                    break;
                default:
                    return text;
            }
            return color + text + RESET;
        }
    }

    private static class DirectFileHandler extends StreamHandler {
        DirectFileHandler(File file) throws IOException {
            setOutputStream(Files.newOutputStream(file.toPath()));
            setLevel(Level.ALL);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            flush();
        }
    }

    private static final class SystemOutConsoleHandler extends ConsoleHandler {
        SystemOutConsoleHandler() {
            setFormatter(new FoxLoaderConsoleLogFormatter());
            setLevel(Level.ALL);
        }

        @Override
        protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
        	super.setOutputStream(currentSystemOut);
        }
    }

    private static final class WrappedSystemOutConsoleHandler extends ConsoleHandler {
        @Override
        public void publish(LogRecord record) {
            systemOutConsoleHandler.publish(record);
        }

        @Override
        public synchronized void flush() {
            // flush() may be called before "systemOutConsoleHandler" is set
            SystemOutConsoleHandler systemOutConsoleHandler =
                    LoggerHelper.systemOutConsoleHandler;
            if (systemOutConsoleHandler != null) {
                systemOutConsoleHandler.flush();
            }
        }

        @Override
        public void close() {}

        @Override
        public ErrorManager getErrorManager() {
            return systemOutConsoleHandler.getErrorManager();
        }

        @Override
        public Filter getFilter() {
            return systemOutConsoleHandler.getFilter();
        }

        @Override
        public Formatter getFormatter() {
            return systemOutConsoleHandler.getFormatter();
        }

        @Override
        public Level getLevel() {
            return systemOutConsoleHandler.getLevel();
        }

        @Override
        public String getEncoding() {
            return systemOutConsoleHandler.getEncoding();
        }

        @Override
        public synchronized void setEncoding(String encoding) throws SecurityException {}

        @Override
        protected synchronized void setOutputStream(OutputStream out) throws SecurityException {}

        @Override
        public synchronized void setFilter(Filter newFilter) throws SecurityException {}

        @Override
        public synchronized void setFormatter(Formatter newFormatter) throws SecurityException {}

        @Override
        public synchronized void setLevel(Level newLevel) throws SecurityException {}
    }

    private static class CantInstallLoggerHelperException extends Exception {
        CantInstallLoggerHelperException(String message) {
            super(message);
        }
    }
}
