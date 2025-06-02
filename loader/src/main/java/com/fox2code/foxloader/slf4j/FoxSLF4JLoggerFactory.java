package com.fox2code.foxloader.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;

final class FoxSLF4JLoggerFactory implements ILoggerFactory {
    private final HashMap<String, FoxSLF4JLogger> loggers = new HashMap<>();

    @Override
    public Logger getLogger(String name) {
        return this.loggers.computeIfAbsent(name, k ->
                new FoxSLF4JLogger(java.util.logging.Logger.getLogger(k)));
    }
}
