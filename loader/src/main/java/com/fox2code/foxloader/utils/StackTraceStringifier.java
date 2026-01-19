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
package com.fox2code.foxloader.utils;

import com.fox2code.foxloader.utils.async.FastThreadLocal;
import org.jetbrains.annotations.NotNull;

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