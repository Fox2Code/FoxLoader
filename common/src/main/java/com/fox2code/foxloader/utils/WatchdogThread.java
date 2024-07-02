package com.fox2code.foxloader.utils;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

final class WatchdogThread extends Thread {
    private static final long T1_SECONDS  =  1000L;
    private static final long T1_9_SECONDS  =  1900L;
    private static WatchdogThread watchdogThread;
    private final CopyOnWriteArrayList<WeakReference<WatchdogTimer>> watchdogTimers;
    private long lastStackDump;

    private WatchdogThread() {
        this.setDaemon(true);
        this.setPriority(Thread.MIN_PRIORITY);
        this.setName("FoxLoader - Watchdog Thread");
        this.watchdogTimers = new CopyOnWriteArrayList<>();
        this.lastStackDump = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(new Thread(
                this::interrupt, "FoxLoader - Watchdog Shutdown Thread"));
    }

    @Override
    public void run() {
        try {
            boolean exit = false;
            while (!(exit || this.isInterrupted())) {
                //noinspection BusyWait
                Thread.sleep(T1_SECONDS);
                boolean needDump = false;
                for (WeakReference<WatchdogTimer> watchdogTimerRef : this.watchdogTimers) {
                    WatchdogTimer watchdogTimer = watchdogTimerRef.get();
                    if (watchdogTimer != null && watchdogTimer.isEnabled()) {
                        long computingFor = watchdogTimer.getComputingFor();
                        if (computingFor / T1_SECONDS == 20L) {
                            needDump = true;
                            if (watchdogTimer.isEssential()) {
                                exit = true;
                            }
                        } else if (computingFor / T1_SECONDS == 10L) {
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
            System.exit(1);
        }
    }
}
