package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.ServerModLoader;
import com.fox2code.foxloader.loader.packet.ServerDynamicTexture;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.game.level.ISaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Shadow private static MinecraftServer theMinecraft;
    @Unique private boolean hasServerStarted;

    @Inject(method = "run", at = @At("HEAD"))
    public void onRun(CallbackInfo ci) {
        theMinecraft = (MinecraftServer) (Object) this;
        ServerModLoader.Internal.notifyRun();
    }

    @Inject(method = "doTick", at = @At("HEAD"))
    public void onRunTick(CallbackInfo ci) {
        ModLoader.Internal.notifyOnTick();
    }


    @Inject(method = "startServer", at = @At("RETURN"))
    public void onStartServer(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == Boolean.TRUE) {
            this.hasServerStarted = true;
            ModLoader.Internal.notifyOnServerStart(
                    NetworkPlayer.ConnectionType.SERVER_ONLY);
        }
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    public void onStopServer(CallbackInfo ci) {
        if (this.hasServerStarted) {
            ModLoader.Internal.notifyOnServerStop(
                    NetworkPlayer.ConnectionType.SERVER_ONLY);
        }
    }

    @Inject(method = "initWorld", at = @At("RETURN"))
    public void initWorldHook(ISaveFormat var1, String var2, long var3, CallbackInfo ci) {
        try {
            ServerModLoader.dynamicTexturesPackets = ServerDynamicTexture.readFromWorld(new File(var2));
            ModLoader.getModLoaderLogger().info("Loaded " +
                    ServerModLoader.dynamicTexturesPackets.size() + " dynamic textures!");
        } catch (IOException e) {
            ModLoader.getModLoaderLogger().log(Level.WARNING, "Failed to load dynamic textures!", e);
            ServerModLoader.dynamicTexturesPackets = Collections.emptyList();
        }
    }
}
