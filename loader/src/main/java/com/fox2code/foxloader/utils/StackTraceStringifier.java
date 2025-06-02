package com.fox2code.foxloader.utils;

import com.fox2code.foxloader.utils.async.FastThreadLocal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public final class StackTraceStringifier {
    private final StringBuilder stringBuilder = new StringBuilder(2048);
    private final PrintWriter printWriter = new PrintWriter(new Writer() {
        @Override
        public void write(int c) {
            stringBuilder.append((char) c);
        }

        @Override
        public void write(char @NotNull [] cbuf, int off, int len) {
            stringBuilder.append(cbuf, off, len);
        }

        @Override
        public void write(@NotNull String str, int off, int len) {
            stringBuilder.append(str, off, len);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }, false);
    private Throwable lastThrowable;

    // We only have one instance per thread.
    // So let's have thread unsafe code here
    public String stringify(Throwable throwable) {
        if (lastThrowable == throwable) {
            return stringBuilder.toString();
        }
        stringBuilder.setLength(0);
        throwable.printStackTrace(new PrintWriter(this.printWriter));
        this.printWriter.flush();
        stringBuilder.setLength(
                stringBuilder.length() - 1);
        lastThrowable = throwable;
        return stringBuilder.toString();
    }

    private static final FastThreadLocal<StackTraceStringifier> stringifier =
            FastThreadLocal.withInitial(StackTraceStringifier::new);

    public static String stringifyStackTrace(Throwable throwable) {
        return stringifier.get().stringify(throwable);
    }
}