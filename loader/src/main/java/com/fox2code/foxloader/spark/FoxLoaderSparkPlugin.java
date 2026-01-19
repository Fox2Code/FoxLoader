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
package com.fox2code.foxloader.spark;

import com.fox2code.foxevents.EventHandler;
import com.fox2code.foxloader.event.GlobalTickEvent;
import com.fox2code.foxloader.event.lifecycle.LifecycleStartEvent;
import com.fox2code.foxloader.event.lifecycle.LifecycleStopEvent;
import com.fox2code.foxloader.launcher.BuildConfig;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.Mod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ModLoaderInit;
import com.fox2code.foxloader.registry.CommandRegistry;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.SparkThreadFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.common.command.ICommandListener;
import net.minecraft.common.command.IllegalCmdListenerOperation;
import net.minecraft.common.command.completion.*;
import net.minecraft.common.util.ChatColors;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class FoxLoaderSparkPlugin extends Mod implements SparkPlugin {
    private static final File sparkConfigFile = new File(ModLoader.getConfigFolder(), "spark");
    private static final Path sparkConfigPath = sparkConfigFile.toPath();
    private final ThreadDumper.GameThread gameThreadDumper;
    private final SparkThreadFactory sparkThreadFactory;
    ScheduledExecutorService scheduler;
    private final PlatformInfo platformInfo;
    SparkPlatform platform;
    private final Logger logger;
    FoxLoaderSparkTickHook tickHook;
    private boolean enabled = false;

    public FoxLoaderSparkPlugin() {
        PlatformInfo.Type type = PlatformInfo.Type.valueOf(FoxLauncher.getEnvironmentType().name());
        this.gameThreadDumper = new ThreadDumper.GameThread(ModLoader::getGameThread);
        this.logger = this.getLogger();
        this.sparkThreadFactory = new SparkThreadFactory();
        this.platformInfo = new FoxLoaderSparkPlatformInfo(type);
        if (!sparkConfigFile.exists() && !sparkConfigFile.mkdirs())
            throw new RuntimeException("Cannot create spark config directory");
        final CommandCompletion sparkCommandCompletion = new CommandCompletionTree().withCompletion("profiler",
                new CommandCompletionTree().withCompletion("info").withCompletion("open")
                        .withCompletion("start", new CommandCompletionOptions()
                                .addUniqueOption("--timeout", CommandCompletionAny.INSTANCE)
                                .addUniqueOption("--thread", CommandCompletionAny.INSTANCE)
                                .addUniqueOption("--only-ticks-over", CommandCompletionAny.INSTANCE)
                                .addUniqueOption("--interval", CommandCompletionAny.INSTANCE)
                                .addUniqueOption("--alloc"))
                        .withCompletion("stop").withCompletion("cancel")
        ).withCompletion("tps").withCompletion("ping", new CommandCompletionOptions()
                        .addUniqueOption("--player", CommandCompletionEntity.PLAYER_STRICT))
                .withCompletion("healthreport", new CommandCompletionOptions()
                        .addUniqueOption("--upload").addUniqueOption("--memory").addUniqueOption("--network"))
                .withCompletion("tickmonitor", new CommandCompletionOptions()
                        .addUniqueOption("--threshold", CommandCompletionAny.INSTANCE)
                        .addUniqueOption("--threshold-tick", CommandCompletionAny.INSTANCE)
                        .addUniqueOption("--without-gc"))
                .withCompletion("gc").withCompletion("gcmonitor").withCompletion("heapsummary",
                        new CommandCompletionOptions().addUniqueOption("--save-to-file"))
                .withCompletion("heapdump", new CommandCompletionOptions().addUniqueOption("--compress"))
                .withCompletion("activity", new CommandCompletionOptions()
                        .addUniqueOption("--page", CommandCompletionAny.INSTANCE));
        CommandRegistry.registerCommand(new FoxLoaderSparkCommand("spark", true) {
            @Override
            public void onExecute(String[] args, ICommandListener commandExecutor) throws IllegalCmdListenerOperation {
                runCommand(args, commandExecutor, false);
            }

            @Override
            protected CommandCompletion commandCompletion() {
                return sparkCommandCompletion;
            }
        });
        CommandRegistry.registerClientCommand(new FoxLoaderSparkCommand("sparkclient", false, "sparkc") {
            @Override
            public void onExecute(String[] args, ICommandListener commandExecutor) throws IllegalCmdListenerOperation {
                runCommand(args, commandExecutor, true);
            }

            @Override
            protected CommandCompletion commandCompletion() {
                return sparkCommandCompletion;
            }
        });
        // The "/tps" is technically not a spark feature but a FoxLoader one!
        // But since the command relies on Spark API, might as well put it here
        CommandRegistry.registerCommand(new FoxLoaderSparkCommand("tps", false) {
            private final DecimalFormat formatter = new DecimalFormat("#0.00");

            @Override
            public void onExecute(String[] args, ICommandListener commandExecutor) throws IllegalCmdListenerOperation {
                final TickStatistics stats = platform.getTickStatistics();
                StringBuilder stringBuilder = new StringBuilder(ChatColors.RESET + "TPS from last 1m, 5m, 15m: ");
                formatTps(stringBuilder, stats.tps1Min());
                stringBuilder.append(", ");
                formatTps(stringBuilder, stats.tps5Min());
                stringBuilder.append(", ");
                formatTps(stringBuilder, stats.tps15Min());
                commandExecutor.log(stringBuilder.toString());
            }

            private void formatTps(StringBuilder stringBuilder, double tps) {
                stringBuilder.append(( ( tps > 20.001 ) ? ChatColors.AQUA :
                        ( tps > 18.0 ) ? ChatColors.GREEN : ( tps > 16.0 ) ? ChatColors.YELLOW : ChatColors.RED));
                stringBuilder.append(formatter.format(tps));
                stringBuilder.append(ChatColors.RESET);
            }
        });
    }

    @EventHandler
    public void onServerStart(LifecycleStartEvent connectionType) {
        this.enable();
    }

    @EventHandler
    public void onServerStop(LifecycleStopEvent connectionType) {
        this.disable();
    }

    @EventHandler
    public void onTick(GlobalTickEvent globalTickEvent) {
        FoxLoaderSparkTickHook tickHook = this.tickHook;
        if (tickHook != null) {
            tickHook.callOnTick();
        }
    }

    public void enable() {
        if (this.enabled) {
            return;
        }
        this.enabled = true;
        this.scheduler = Executors.newScheduledThreadPool(4, this.sparkThreadFactory);
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public void disable() {
        if (!this.enabled) {
            return;
        }
        this.enabled = false;
        this.platform.disable();
        this.scheduler.shutdown();
        this.tickHook = null;
    }

    public void runCommand(String[] args, ICommandListener commandExecutor, boolean absolute) {
        this.platform.executeCommand(
                new FoxLoaderSparkCommandSender(commandExecutor, absolute),
                Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public String getVersion() {
        return BuildConfig.SPARK_VERSION;
    }

    @Override
    public Path getPluginDirectory() {
        return sparkConfigPath;
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        switch (FoxLauncher.getEnvironmentType()) {
            case CLIENT:
                return Stream.of(new FoxLoaderSparkCommandSender(Minecraft.getInstance().thePlayer, true));
            case SERVER:
                return MinecraftServer.getInstance().configManager.playerEntities.stream()
                        .map(p -> new FoxLoaderSparkCommandSender(p.playerNetServerHandler));
            default:
                throw new IllegalStateException("What?");
        }
    }

    @Override
    public void executeAsync(Runnable task) {
        try {
            this.scheduler.execute(task);
        } catch (NullPointerException | RejectedExecutionException e) {
            this.getLogger().log(Level.WARNING, "Failed to execute async task", e);
        }
    }

    @Override
    public void log(Level level, String msg) {
        this.logger.log(level, msg);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        this.logger.log(level, msg, throwable);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper.get();
    }

    @Override
    public TickHook createTickHook() {
        return new FoxLoaderSparkTickHook(this);
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new FoxLoaderSparkClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(ModLoaderInit.getModContainers(),
                mod -> mod.getModInfo().name,
                mod -> mod.getModInfo().version,
                mod -> mod.getModInfo().authors,
                mod -> mod.getModInfo().description);
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return FoxLauncher.isServer() ? new FoxLoaderSparkPlayerPingProvider() : null;
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new FoxLoaderSparkServerConfigProvider();
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return this.platformInfo;
    }
}
