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
package com.fox2code.foxloader.dev;

public final class UserMessage extends Throwable {
    public static final UserMessage UNRECOVERABLE_STATE_DECOMPILE = new UserMessage("---",
            "Daemon was in an unrecoverable state for decompile and was terminated.",
            "if after reloading the project the problem persists you can add",
            "\"foxloader.decompileSources = false\" to your build.gradle to disable",
            "ReIndev source code decompilation entirely");
    public static final UserMessage FAIL_DECOMPILE = new UserMessage();

    private UserMessage() {
        this("---",
                "An error happened while trying to decompile ReIndev ",
                "FoxLoader development plugin tried to solve the invalid state for you, but",
                "if after reloading the project the problem persists you can add",
                "\"foxloader.decompileSources = false\" to your build.gradle to disable",
                "ReIndev source code decompilation entirely");
    }

    public UserMessage(String... strings) {
        super(format(strings), null);
    }

    private static String format(String[] strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : strings) {
            stringBuilder.append('\n').append(string);
        }
        return stringBuilder.toString();
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
