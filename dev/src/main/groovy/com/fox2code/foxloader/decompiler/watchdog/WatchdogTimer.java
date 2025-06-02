package com.fox2code.foxloader.decompiler.watchdog;

public final class WatchdogTimer {
    private final boolean essential;
    private volatile long lastCheck;
    private volatile long megaLastCheck;
    private boolean enabled;
    int lastTimeSkip;

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
        return this.lastCheck == 0 ? 0 : System.currentTimeMillis() -
            Math.max(this.lastCheck, this.megaLastCheck);
    }

    public void heartbeat() {
        this.lastCheck = System.currentTimeMillis();
        this.lastTimeSkip = WatchdogThread.lastTimeSkip;
    }

    public void megaHeartbeat() {
        this.megaLastCheck = System.currentTimeMillis() + 20000L;
        this.lastTimeSkip = WatchdogThread.lastTimeSkip;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!this.enabled && enabled &&
                (System.currentTimeMillis() - this.lastCheck) > 2L) {
            this.lastCheck = 0;
        }
        this.enabled = enabled;
    }
}
