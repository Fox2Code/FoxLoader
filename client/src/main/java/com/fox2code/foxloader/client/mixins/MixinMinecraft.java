package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.WorldProviderCustom;
import com.fox2code.foxloader.launcher.FoxLauncher;
import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.network.SidedMetadataAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.GameSettings;
import net.minecraft.src.client.gui.GuiGameOver;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.StringTranslate;
import net.minecraft.src.client.player.EntityPlayerSP;
import net.minecraft.src.client.player.MovementInputFromOptions;
import net.minecraft.src.client.player.PlayerController;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.entity.EntityLiving;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.level.NetherPortalHandler;
import net.minecraft.src.game.level.World;
import net.minecraft.src.game.level.WorldProvider;
import net.minecraft.src.game.level.WorldSettings;
import net.minecraft.src.game.level.chunk.ChunkCoordinates;
import net.minecraft.src.game.level.chunk.ISaveFormat;
import net.minecraft.src.game.level.chunk.ISaveHandler;
import net.minecraft.src.game.stats.StatFileWriter;
import net.minecraft.src.game.stats.StatList;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.fox2code.foxloader.client.WorldProviderCustom.getProviderForDimensioncustom;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow public volatile boolean running;
    @Shadow public GameSettings gameSettings;
    @Shadow private static File minecraftDir;
    @Unique private NetworkPlayer.ConnectionType loadedWorldType;
    @Unique private boolean closeGameDelayed;
    @Unique private boolean showDebugInfoPrevious;
    @Unique public String Dim="notcustom";
    @Unique public String DimR="notcustom";


    @Inject(method = "startGame", at = @At("HEAD"))
    public void onStartGame(CallbackInfo ci) {
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
            if (!world.multiplayerWorld) {
                SidedMetadataAPI.Internal.setActiveMetaData(null);
            }
        }
    }

    @Inject(method = "startMainThread", at = @At("RETURN"))
    private static void onGameStarted(String username, String sessionID, CallbackInfo ci) {
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
    }@Shadow
    public World theWorld;
    @Shadow
    public EntityPlayerSP thePlayer;
    @Shadow
    public EntityLiving renderViewEntity;
    @Shadow
    public abstract void func_6255_d(String arg1);
    @Shadow
    public PlayerController playerController;
    @Shadow

    public abstract void changeWorld(World world, String arg2, EntityPlayer player) ;
    @Inject(method = "respawn",at=@At("TAIL"),cancellable = true)
    public void respawnend(boolean arg1, int arg2,CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {

        this.thePlayer.getClass().getField("customrespawnDimension").set(thePlayer,this.DimR);
        this.thePlayer.getClass().getField("customDimension").set(thePlayer,this.DimR);
        if (this.Dim!="notcustom")thePlayer.dimension=3;

    }
    @Inject(method = "respawn",at=@At("HEAD"),cancellable = true)
    public void respawn(boolean arg1, int arg2,CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        if (!this.theWorld.multiplayerWorld){

            String dimr=(String) thePlayer.getClass().getField("customrespawnDimension").get(thePlayer);
            String dim=(String) thePlayer.getClass().getField("customDimension").get(thePlayer);
            if(dim!=dimr){
                this.theWorld.setEntityDead(this.thePlayer);
                this.thePlayer.isDead = false;
                WorldProvider wp= getProviderForDimensioncustom(dimr=="notcustom"?null:dimr,dimr=="notcustom"?0:3);
                this.Dim=dimr;
                if (this.thePlayer.isEntityAlive()) {
                    this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
                }
                short hc = this.theWorld.worldInfo.getHighestChunkOW();
                short lc = this.theWorld.worldInfo.getLowestChunkOW();
                World world = new World(this.theWorld, wp);
                world.highestChunk = hc;
                world.highestY = hc << 4;
                world.lowestChunk = lc;
                world.lowestY = lc << 4;
                this.changeWorld(world, "going to "+dim, this.thePlayer);
                this.thePlayer.worldObj = this.theWorld;






                ChunkCoordinates chunkCoordinates = null;
                ChunkCoordinates var4 = null;
                boolean var5 = true;
                if (this.thePlayer != null && !arg1) {
                    chunkCoordinates = this.thePlayer.getPlayerSpawnCoordinate();
                    if (chunkCoordinates != null) {
                        System.err.println(chunkCoordinates.x+" "+chunkCoordinates.y+" "+chunkCoordinates.z);
                        System.err.println(
                                "block ID: "+
                                        this.theWorld.getBlockId(chunkCoordinates.x,chunkCoordinates.y,chunkCoordinates.z)+" bed ID: "+ Block.bed.blockID);
                        var4 = EntityPlayer.func_25060_a(this.theWorld, chunkCoordinates);
                        if (var4 == null) {
                            this.thePlayer.addChatMessage("tile.bed.notValid");
                        }
                    }
                }

                if (var4 == null) {
                    var4 = this.theWorld.getSpawnPoint();
                    var5 = false;
                }

                this.theWorld.setSpawnLocation();
                this.theWorld.updateEntityList();
                int var8 = 0;
                if (this.thePlayer != null) {
                    var8 = this.thePlayer.entityId;
                    this.theWorld.setEntityDead(this.thePlayer);
                }

                this.renderViewEntity = null;
                this.thePlayer = (EntityPlayerSP)this.playerController.createPlayer(this.theWorld);
                this.thePlayer.dimension = arg2;
                this.renderViewEntity = this.thePlayer;

                this.thePlayer.preparePlayerToSpawn();
                if (var5) {
                    this.thePlayer.setPlayerSpawnCoordinate(chunkCoordinates);
                    this.thePlayer
                            .setLocationAndAngles(
                                    (double)((float)var4.x + 0.5F),
                                    (double)((float)var4.y + 0.1F),
                                    (double)((float)var4.z + 0.5F),
                                    0.0F,
                                    0.0F
                            );
                }

                this.playerController.flipPlayer(this.thePlayer);
                this.theWorld.spawnPlayerWithLoadedChunks(this.thePlayer);
                this.thePlayer.movementInput = new MovementInputFromOptions(this.gameSettings);
                this.thePlayer.entityId = var8;
                this.thePlayer.func_6420_o();
                this.playerController.func_6473_b(this.thePlayer);
                this.func_6255_d(StringTranslate.getInstance().translateKey("gui.world.respawn"));
                if (this.currentScreen instanceof GuiGameOver) {
                    this.displayGuiScreen((GuiScreen)null);
                }
                ci.cancel();

            }

        }
    }
    @Shadow

    public abstract void displayGuiScreen(GuiScreen gui);
    @Shadow
    public GuiScreen currentScreen ;

    @Inject(method = "usePortal",at=@At("HEAD"),cancellable = true)
    public void usePortal(CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException{

        if (thePlayer.getClass().getField("incustomportal").getBoolean(thePlayer)){
            this.theWorld.setEntityDead(this.thePlayer);
            this.thePlayer.isDead = false;
            if(thePlayer.dimension==0){
                String dim=(String) thePlayer.getClass().getField("goingtodim").get(thePlayer);
                thePlayer.dimension=3;

                WorldProviderCustom wp= (WorldProviderCustom) getProviderForDimensioncustom(dim,3);

                if (this.thePlayer.isEntityAlive()) {
                    this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
                }
                short hc = this.theWorld.worldInfo.getHighestChunkOW();
                short lc = this.theWorld.worldInfo.getLowestChunkOW();
                World world = new World(this.theWorld, wp);
                world.highestChunk = hc;
                world.highestY = hc << 4;
                world.lowestChunk = lc;
                world.lowestY = lc << 4;
                this.Dim=dim;
                this.changeWorld(world, "going to "+dim, this.thePlayer);


            }
            else {
                String dim=(String) thePlayer.getClass().getField("goingtodim").get(thePlayer);
                this.thePlayer.setLocationAndAngles(this.thePlayer.posX, this.thePlayer.posY, this.thePlayer.posZ, this.thePlayer.rotationYaw, this.thePlayer.rotationPitch);
                if (this.thePlayer.isEntityAlive()) {
                    this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
                }

                thePlayer.dimension=0;
                this.Dim="notcustom";
                short hc = this.theWorld.worldInfo.getHighestChunkOW();
                short lc = this.theWorld.worldInfo.getLowestChunkOW();
                World world = new World(this.theWorld, WorldProvider.getProviderForDimension(0));
                world.highestChunk = hc;
                world.highestY = hc << 4;
                world.lowestChunk = lc;
                world.lowestY = lc << 4;
                this.changeWorld(world, "leaving "+dim, this.thePlayer);
            }

            this.thePlayer.worldObj = this.theWorld;
            if (this.thePlayer.isEntityAlive()) {
                this.thePlayer
                        .setLocationAndAngles(
                                thePlayer.posX, this.thePlayer.posY, thePlayer.posZ, this.thePlayer.rotationYaw, this.thePlayer.rotationPitch
                        );
                this.theWorld.updateEntityWithOptionalForce(this.thePlayer, false);
                new NetherPortalHandler().teleportEntity(this.theWorld, this.thePlayer);
            }
            ci.cancel();
        }else if (!thePlayer.getClass().getField("incustomportal").getBoolean(thePlayer)&&thePlayer.dimension==3){
            ci.cancel();
        }
    }
    @Shadow
    private ISaveFormat saveLoader;
    @Shadow
    public abstract void changeWorld1(World world);
    @Shadow
    public abstract void changeWorld2(World world,String arg2);
    @Shadow
    public abstract void convertMapFormat(String arg1, String arg2);
    @Shadow
    public StatFileWriter statFileWriter;



    @Overwrite
    public void startWorld(String arg1, String arg2, WorldSettings worldInfo) {
        this.changeWorld1((World)null);
        System.gc();
        if (this.saveLoader.isOldSaveType(arg1)) {
            this.convertMapFormat(arg1, arg2);
        } else {


            ISaveHandler saveHandler = this.saveLoader.getSaveLoader(arg1, false);
            World world = null;
            if (saveHandler.loadWorldInfo()!=null) {
                this.Dim = saveHandler.loadWorldInfo().getPlayerNBTTagCompound().getString("customDimension");
                this.DimR = saveHandler.loadWorldInfo().getPlayerNBTTagCompound().getString("customrespawnDimension");
            }

            world = new World(saveHandler, arg2, worldInfo, WorldProviderCustom.getProviderForDimensioncustom(this.Dim,3));

            if (world.isNewWorld) {
                this.statFileWriter.readStat(StatList.createWorldStat, 1);
                this.statFileWriter.readStat(StatList.startGameStat, 1);
                this.changeWorld2(world, StringTranslate.getInstance().translateKey("gui.world.generating"));
            } else {
                this.statFileWriter.readStat(StatList.loadWorldStat, 1);
                this.statFileWriter.readStat(StatList.startGameStat, 1);
                this.changeWorld2(world, StringTranslate.getInstance().translateKey("gui.world.loading"));
            }
        }
    }
}
