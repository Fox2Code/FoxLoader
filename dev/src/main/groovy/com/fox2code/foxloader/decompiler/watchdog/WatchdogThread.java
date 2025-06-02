package com.fox2code.foxloader.decompiler.watchdog;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

final class WatchdogThread extends Thread {
    private static final long T1_SECONDS  =  1000L;
    private static final long T1_5_SECONDS  =  1900L;
    private static final long T1_9_SECONDS  =  1900L;
    private static final long SECONDS_BEFORE_DUMP = 10L;
    private static final long SECONDS_BEFORE_FREEZE = 20L;
    private static WatchdogThread watchdogThread;
    private final CopyOnWriteArrayList<WeakReference<WatchdogTimer>> watchdogTimers;
    private long lastStackDump;
    static int lastTimeSkip;

    private WatchdogThread() {
        this.setDaemon(true);
        this.setPriority(Thread.MIN_PRIORITY);
        this.setName("ReIndev - Watchdog Thread");
        this.watchdogTimers = new CopyOnWriteArrayList<>();
        this.lastStackDump = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(new Thread(
                this::interrupt, "ReIndev - Watchdog Shutdown Thread"));
    }

    @Override
    public void run() {
        try {
            boolean exit = false;
            while (!(exit || this.isInterrupted())) {
                long beforeSleep = System.currentTimeMillis();
                //noinspection BusyWait
                Thread.sleep(T1_SECONDS);
                long time = System.currentTimeMillis();
                // Check if system time changed, as we don't want to crash if system time changed.
                long diff = time - beforeSleep;
                if (diff < 0 || diff > T1_5_SECONDS) {
                    lastTimeSkip++;
                }
                boolean needDump = false;
                Iterator<WeakReference<WatchdogTimer>> watchdogTimerItr = this.watchdogTimers.iterator();
                while (watchdogTimerItr.hasNext()) {
                    WeakReference<WatchdogTimer> watchdogTimerRef = watchdogTimerItr.next();
                    WatchdogTimer watchdogTimer = watchdogTimerRef.get();
                    if (watchdogTimer == null) {
                        watchdogTimerItr.remove();
                    } else if (watchdogTimer.isEnabled() &&
                        watchdogTimer.lastTimeSkip == lastTimeSkip) {
                        long computingFor = watchdogTimer.getComputingFor();
                        if (computingFor / T1_SECONDS == SECONDS_BEFORE_FREEZE) {
                            needDump = true;
                            if (watchdogTimer.isEssential()) {
                                exit = true;
                            }
                        } else if (computingFor / T1_SECONDS == SECONDS_BEFORE_DUMP) {
                            needDump = true;
                        }
                    }
                }
                if (needDump) {
                    doCompleteThreadDump(exit);
                }
            }
        } catch (InterruptedException ignored) {}
    }

    static void registerTimer(WatchdogTimer watchdogTimer) {
        if (watchdogThread == null) {
            watchdogThread = new WatchdogThread();
            watchdogThread.start();
        }
        watchdogThread.watchdogTimers.add(new WeakReference<>(watchdogTimer));
    }

    private void doCompleteThreadDump(boolean andExit) {
        if (System.currentTimeMillis() - this.lastStackDump < T1_9_SECONDS && !andExit) return;
        this.lastStackDump = System.currentTimeMillis();
        System.out.println((andExit ? "Fatal freeze" : "Freeze") + " detected, creating stack dump:");
        System.out.println();
        for (Map.Entry<Thread, StackTraceElement[]> stackTrace : Thread.getAllStackTraces().entrySet()) {
            Thread thread = stackTrace.getKey();
            System.out.println(thread.getName() + " (Priority: " +
                    thread.getPriority() + (thread.isDaemon() ? ", Daemon)" : ")"));
            for (StackTraceElement stackTraceElement : stackTrace.getValue()) {
                System.out.println("    " + stackTraceElement.toString());
            }
            System.out.println();
        }
        if (andExit) {
            System.out.flush();
            System.exit(1);
        }
    }
}
