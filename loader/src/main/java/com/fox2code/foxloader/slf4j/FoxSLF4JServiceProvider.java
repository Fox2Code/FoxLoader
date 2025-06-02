package com.fox2code.foxloader.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public final class FoxSLF4JServiceProvider implements SLF4JServiceProvider {
    private final ILoggerFactory loggerFactory;
    private final IMarkerFactory  markerFactory;
    private final MDCAdapter mdcAdapter;

    public FoxSLF4JServiceProvider() {
        this.loggerFactory = new FoxSLF4JLoggerFactory();
        this.markerFactory = new BasicMarkerFactory();
        this.mdcAdapter =  new NOPMDCAdapter();
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return this.markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return this.mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }

    @Override
    public void initialize() {}
}
