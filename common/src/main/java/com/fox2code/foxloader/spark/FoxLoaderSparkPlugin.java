package com.fox2code.foxloader.spark;

import com.fox2code.foxloader.loader.Mod;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.ChatColors;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import me.lucko.spark.api.Spark;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.api.SparkApi;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.SparkThreadFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class FoxLoaderSparkPlugin extends Mod implements SparkPlugin {
    private static final File sparkConfigFile = new File(ModLoader.config, "spark");
    private static final Path sparkConfigPath = sparkConfigFile.toPath();
    private final ThreadDumper.GameThread gameThreadDumper;
    private final SparkThreadFactory sparkThreadFactory;
    protected ScheduledExecutorService scheduler;
    private final PlatformInfo platformInfo;
    protected SparkPlatform platform;
    private final Logger logger;
    FoxLoaderSparkTickHook tickHook;

    public FoxLoaderSparkPlugin(PlatformInfo.Type type) {
        this.gameThreadDumper = new ThreadDumper.GameThread(ModLoader::getGameThread);
        this.logger = this.getLogger();
        this.sparkThreadFactory = new SparkThreadFactory();
        this.platformInfo = new FoxLoaderSparkPlatformInfo(type);
        if (!sparkConfigFile.exists() && !sparkConfigFile.mkdirs())
            throw new RuntimeException("Cannot create spark config directory");
        CommandCompat.registerCommand(new CommandCompat("spark") {
            @Override
            public void onExecute(String[] args, NetworkPlayer commandExecutor) {
                runCommand(args, commandExecutor);
            }
        });
        // The "/tps" is technically not a spark feature but a FoxLoader one!
        // But since the command relies on Spark API, might as well put it here
        CommandCompat.registerCommand(new CommandCompat("tps", false) {
            @Override
            public void onExecute(String[] args, NetworkPlayer commandExecutor) {
                final TickStatistics stats = platform.getTickStatistics();
                StringBuilder stringBuilder = new StringBuilder("TPS from last 1m, 5m, 15m: ");
                formatTps(stringBuilder, stats.tps1Min());
                stringBuilder.append(", ");
                formatTps(stringBuilder, stats.tps5Min());
                stringBuilder.append(", ");
                formatTps(stringBuilder, stats.tps15Min());
                commandExecutor.displayChatMessage(stringBuilder.toString());
            }

            private void formatTps(StringBuilder stringBuilder, double tps) {
                stringBuilder.append(( ( tps > 20.0 ) ? ChatColors.RAINBOW :
                        ( tps > 18.0 ) ? ChatColors.GREEN : ( tps > 16.0 ) ? ChatColors.YELLOW : ChatColors.RED));
                stringBuilder.append(tps);
                stringBuilder.append(ChatColors.RESET);
            }
        });
    }

    @Override
    public void onServerStart(NetworkPlayer.ConnectionType connectionType) {
        if (connectionType.isServer) this.enable();
    }

    @Override
    public void onServerStop(NetworkPlayer.ConnectionType connectionType) {
        if (connectionType.isServer) this.disable();
    }

    public void enable() {
        this.scheduler = Executors.newScheduledThreadPool(4, this.sparkThreadFactory);
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
        this.tickHook = null;
    }

    public void runCommand(String[] args, NetworkPlayer commandExecutor) {
        this.platform.executeCommand(
                new FoxLoaderSparkCommandSender(commandExecutor),
                Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public String getVersion() {
        return null;
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
    public void executeAsync(Runnable task) {
        try {
            this.scheduler.execute(task);
        } catch (NullPointerException | RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void log(Level level, String s) {
        this.logger.log(level, s);
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
    public PlatformInfo getPlatformInfo() {
        return this.platformInfo;
    }
}
