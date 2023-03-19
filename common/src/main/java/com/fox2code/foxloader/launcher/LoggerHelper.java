package com.fox2code.foxloader.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.logging.*;

final class LoggerHelper {
    private static final String format = "[%1$tT] [%2$-7s] %3$s";
    private static final Level STDOUT = new Level("STDOUT", 800, Level.INFO.getResourceBundleName()) {};
    private static final Level STDERR = new Level("STDERR", 1000, Level.INFO.getResourceBundleName()) {};
    static boolean consoleSupportColor = Boolean.getBoolean("foxloader.dev-mode") || // Assume true if dev-mode.
            FoxLauncher.foxLoaderFile.getAbsolutePath().replace('\\', '/').endsWith( // Also check for IDE launch.
                    "/common/build/libs/common-" + BuildConfig.FOXLOADER_VERSION + ".jar");

    static boolean install(File logFile) {
        if (System.out.getClass() != PrintStream.class) {
            // If System.out already has been replaced just ignore the replacement.
            return false;
        }
        boolean installed = false;
        final SystemOutConsoleHandler systemOutConsoleHandler = new SystemOutConsoleHandler();
        final FoxLoaderLogFormatter simpleFormatter = new FoxLoaderLogFormatter();
        final Logger rootLogger = LogManager.getLogManager().getLogger("");
        try {
            rootLogger.addHandler(new DirectFileHandler(logFile));
        } catch (Exception ignored) {
            return false;
        }
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                installed = true;
                rootLogger.removeHandler(handler);
                rootLogger.addHandler(systemOutConsoleHandler);
            } else {
                handler.setFormatter(simpleFormatter);
            }
        }
        if (installed) {
            final PrintStream out = System.out;
            out.flush(); // <- Make sure buffer is flushed
            System.setOut(new FoxLoaderLogPrintStream(out, rootLogger, STDOUT, false));
            System.setErr(new FoxLoaderLogPrintStream(out, rootLogger, STDERR, true));
        }
        return installed;
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
            Throwable throwable = lr.getThrown();
            if (throwable != null) {
                message += stringifyStackTrace(throwable);
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
                default:
                    return text;
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
            }
            return color + text + RESET;
        }
    }

    private static class DirectFileHandler extends StreamHandler {
        DirectFileHandler(File file) throws IOException {
            setOutputStream(Files.newOutputStream(file.toPath()));
        }
    }

    private static class SystemOutConsoleHandler extends ConsoleHandler {
        SystemOutConsoleHandler() {
            setOutputStream(System.out);
            setFormatter(new FoxLoaderConsoleLogFormatter());
            setLevel(Level.ALL);
        }
    }

    private static class StackTraceStringifier {
        private final StringBuilder stringBuilder = new StringBuilder(2048).append("\n");
        private final PrintWriter printWriter = new PrintWriter(new Writer() {
            @Override
            public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                stringBuilder.append(cbuf, off, len);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        }, false);
        private Throwable lastThrowable;

        // We only have one instance per thread.
        // So let's have thread unsafe code here
        String stringify(Throwable throwable) {
            if (lastThrowable == throwable) {
                return stringBuilder.toString();
            }
            stringBuilder.setLength(1);
            throwable.printStackTrace(new PrintWriter(this.printWriter));
            this.printWriter.flush();
            stringBuilder.setLength(
                    stringBuilder.length() - 1);
            lastThrowable = throwable;
            return stringBuilder.toString();
        }
    }

    private static final ThreadLocal<StackTraceStringifier> stringifier =
            ThreadLocal.withInitial(StackTraceStringifier::new);
    private static String stringifyStackTrace(Throwable throwable) {
        return stringifier.get().stringify(throwable);
    }
}
