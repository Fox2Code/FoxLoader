package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.StringTranslate;
import net.minecraft.src.client.player.EntityPlayerSP;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends EntityPlayer implements NetworkPlayer {

    @Shadow public Minecraft mc;

    public MixinEntityPlayerSP(World var1) {
        super(var1);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SINGLE_PLAYER;
    }

    @Override
    public void sendNetworkData(ModContainer modContainer, byte[] data) {}

    @Override
    public void displayChatMessage(String chatMessage) {
        StringTranslate st = StringTranslate.getInstance();
        if (chatMessage.indexOf('\n') == -1) {
            Minecraft.getInstance().ingameGUI.addChatMessage(st.translateKey(chatMessage));
        } else {
            String[] splits = chatMessage.split("\\n");
            for (String split : splits) {
                Minecraft.getInstance().ingameGUI.addChatMessage(st.translateKey(split));
            }
        }
    }

    @Override
    public boolean hasFoxLoader() {
        return true;
    }

    @Override
    public String getPlayerName() {
        return ((EntityPlayer) (Object) this).username;
    }

    @Override
    public boolean isOperator() {
        return ((Entity) (Object) this).worldObj.worldInfo.isCheatsEnabled();
    }

    @Override
    public void kick(String message) {
        throw new IllegalStateException("kick cannot be used client-side");
    }

    @Override
    public NetworkPlayerController getNetworkPlayerController() {
        return (NetworkPlayerController) Minecraft.getInstance().playerController;
    }

    @Override
    public boolean isConnected() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.theWorld != null && !mc.isMultiplayerWorld();
    }

    @Override
    public RegisteredItemStack getRegisteredHeldItem() {
        EntityPlayerSP networkPlayerSP = (EntityPlayerSP) (Object) this;
        return ClientMod.toRegisteredItemStack(networkPlayerSP.inventory.getCurrentItem());
    }

    @Override
    public void sendPlayerThroughPortalRegistered() {
        this.mc.usePortal();
        this.inPortal = false;
    }
}
