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
package com.fox2code.foxloader.slf4j;

import com.fox2code.foxloader.utils.CustomLogLevel;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.LegacyAbstractLogger;

import java.util.logging.Level;
import java.util.logging.LogRecord;

final class FoxSLF4JLogger extends LegacyAbstractLogger implements Logger {
    private final java.util.logging.Logger logger;

    FoxSLF4JLogger(java.util.logging.Logger logger) {
        this.logger = logger;
        this.name = logger.getName();
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(org.slf4j.event.Level slf4jLevel, Marker marker,
                                               String messagePattern, Object[] arguments, Throwable throwable) {
        Level level = null;
        switch (slf4jLevel) {
            case ERROR:
                level = CustomLogLevel.ERROR;
                break;
            case WARN:
                level = CustomLogLevel.WARNING;
                break;
            case INFO:
                level = CustomLogLevel.INFO;
                break;
            case DEBUG:
                level = CustomLogLevel.DEBUG;
                break;
            case TRACE:
                level = CustomLogLevel.TRACE;
                break;
        }
        if (!this.logger.isLoggable(level)) return;
        LogRecord logRecord = new LogRecord(level, messagePattern);
        logRecord.setSourceClassName(null);
        logRecord.setLoggerName(this.name);
        logRecord.setParameters(arguments);
        logRecord.setThrown(throwable);
        this.logger.log(logRecord);
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isLoggable(CustomLogLevel.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isLoggable(CustomLogLevel.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isLoggable(CustomLogLevel.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isLoggable(CustomLogLevel.WARNING);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isLoggable(CustomLogLevel.ERROR);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return this.logger.isLoggable(CustomLogLevel.ERROR);
    }
}
