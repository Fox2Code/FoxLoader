package com.fox2code.foxloader.server;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.CommandSender;
import net.minecraft.server.command.IssuerRole;
import net.minecraft.server.plugin.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Compatibility class used in ReIndevPatches
 */
public class ServerCommandWrapper4ReIndevPatches extends Command {
    private static final Method getPlayer;
    static  {
        Method getPlayerTmp;
        try {
            //noinspection JavaReflectionMemberAccess
            getPlayerTmp = CommandSender.class.getDeclaredMethod("getPlayer");
        } catch (NoSuchMethodException e) {
            try {
                //noinspection JavaReflectionMemberAccess
                getPlayerTmp = CommandSender.class.getDeclaredMethod("getCommandSender");
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException("Not ReIndevPatches?", e);
            }
        }
        getPlayer = getPlayerTmp;
    }

    private final CommandCompat commandCompat;

    private ServerCommandWrapper4ReIndevPatches(CommandCompat commandCompat) {
        super(commandCompat.getName(), commandCompat.getAliases());
        this.setIssuerRole(IssuerRole.PLAYER); // We don't support console :(
        this.commandCompat = commandCompat;
    }

    @Override
    public void execute(String commandLabel, String[] args, CommandSender commandSender) {
        NetworkPlayer networkPlayer = null;
        if (commandSender.isPlayer()) {
            try {
                networkPlayer = (NetworkPlayer) getPlayer.invoke(commandSender);
            } catch (Exception ignored) {}
        }
        if (networkPlayer == null) {
            commandSender.sendMessage("FoxLoader defined commands can only be executed by players!");
            return;
        }
        final String[] foxArgs = new String[args.length + 1];
        foxArgs[0] = commandLabel;
        System.arraycopy(args, 0, foxArgs, 1, args.length);
        this.commandCompat.onExecute(foxArgs, networkPlayer);
    }

    @Override
    public boolean onlyForOperators() {
        return this.commandCompat.isOpOnly();
    }

    @Override
    public boolean hideCommandArgs() {
        return this.commandCompat.isHidden();
    }

    private static final JavaPlugin foxLoaderJavaPlugin = new JavaPlugin() {
        @SuppressWarnings("deprecation")
        @Override
        public String getName() {
            return "FoxLoader";
        }
    };

    public static void registerAllForReIndevPatches() {
        if (!ModLoader.areAllModsLoaded()) // Just a safeguard, just in case.
            throw new IllegalArgumentException("Not Loaded?");
        for (CommandCompat commandCompat : CommandCompat.commands.values()) {
            if (!foxLoaderJavaPlugin.registerCommand(new ServerCommandWrapper4ReIndevPatches(commandCompat), true)) {
                ModLoader.getModLoaderLogger().warning("Failed to register /" + commandCompat.getName() + " command");
            }
        }
    }
}
