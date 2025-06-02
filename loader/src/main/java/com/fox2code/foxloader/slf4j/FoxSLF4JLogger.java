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
