package com.fox2code.foxloader.utils;

public final class WatchdogTimer {
    private final boolean essential;
    private volatile long lastCheck;
    private volatile long megaLastCheck;
    private boolean enabled;

    public WatchdogTimer(boolean essential) {
        WatchdogThread.registerTimer(this);
        this.essential = essential;
        this.lastCheck = 0;
        this.megaHeartbeat();
        this.enabled = true;
    }

    public boolean isEssential() {
        return this.essential;
    }

    long getComputingFor() {
        return this.lastCheck == 0 ? 0 : System.currentTimeMillis() - Math.max(this.lastCheck, this.megaLastCheck);
    }

    public void heartbeat() {
        this.lastCheck = System.currentTimeMillis();
    }

    public void megaHeartbeat() {
        this.megaLastCheck = System.currentTimeMillis() + 10000L;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
