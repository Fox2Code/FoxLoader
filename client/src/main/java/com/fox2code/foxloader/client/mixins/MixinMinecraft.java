package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.GameSettings;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.level.World;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow public volatile boolean running;
    @Shadow public GameSettings gameSettings;
    @Shadow private static File minecraftDir;
    @Unique private NetworkPlayer.ConnectionType loadedWorldType;
    @Unique private boolean closeGameDelayed;
    @Unique private boolean showDebugInfoPrevious;

    @Inject(method = "run", at = @At("HEAD"))
    public void onRun(CallbackInfo ci) {
        ClientModLoader.Internal.notifyRun();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void onRunTick(CallbackInfo ci) {
        ModLoader.Internal.notifyOnTick();
    }

    @Inject(method = "changeWorld", at = @At("RETURN"))
    public void onChangeWorld(World world, String var2, EntityPlayer player, CallbackInfo ci) {
        if (world == null) {
            if (loadedWorldType != null) {
                NetworkPlayer.ConnectionType
                        tmp = this.loadedWorldType;
                this.loadedWorldType = null;
                ModLoader.Internal.notifyOnServerStop(tmp);
            }
        } else if (loadedWorldType == null) {
            ModLoader.Internal.notifyOnServerStart(
                    this.loadedWorldType = world.multiplayerWorld ?
                            NetworkPlayer.ConnectionType.CLIENT_ONLY :
                            NetworkPlayer.ConnectionType.SINGLE_PLAYER);
        }
    }

    @Inject(method = "startMainThread", at = @At("RETURN"))
    private static void onGameStarted(String var0, String var1, String var2, CallbackInfo ci) {
        try {
            Frame[] frames = Frame.getFrames();
            final List<Image> icons = Collections.singletonList(
                    ImageIO.read(Objects.requireNonNull(Minecraft.class.getResource("/icon.png"))));
            for (Frame frame : frames) {
                try {
                    frame.setIconImages(icons);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "getMinecraftDir", at = @At("HEAD"))
    private static void onGetMinecraftDir(CallbackInfoReturnable<File> cir) {
        if (minecraftDir == null) {
            minecraftDir = FoxLauncher.getGameDir();
        }
    }

    // Linux fix:
    @Inject(method = "shutdown", at = @At(value = "HEAD"), cancellable = true)
    public void shutdownRedirect(CallbackInfo ci) {
        if (this.running && ClientModLoader.linuxFix) {
            this.closeGameDelayed = true;
            ci.cancel();
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    public void onRunTickEnd(CallbackInfo ci) {
        if (this.showDebugInfoPrevious != this.gameSettings.showDebugInfo) {
            boolean debugEnabled = this.showDebugInfoPrevious = this.gameSettings.showDebugInfo;
            if (debugEnabled) { // F3 + Maj show time
                ClientModLoader.showFrameTimes = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
            } else ClientModLoader.showFrameTimes = false;
        }
        if (this.closeGameDelayed) {
            this.closeGameDelayed = false;
            this.running = false;
        }
    }
}
