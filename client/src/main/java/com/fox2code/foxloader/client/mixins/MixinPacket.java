package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.network.NetworkItemStack;
import com.fox2code.foxloader.client.network.Packet250PluginMessage;
import com.fox2code.foxloader.network.io.NetworkDataInputStream;
import com.fox2code.foxloader.network.io.NetworkDataOutputStream;
import com.fox2code.foxloader.registry.GameRegistryClient;
import net.minecraft.src.client.packets.Packet;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Mixin(Packet.class)
public abstract class MixinPacket {
    @Shadow static void addIdClassMapping(int var0, boolean var1, boolean var2, Class<? extends Packet> var3) {}

    @Shadow protected static NBTTagCompound readNbtTagCompound(DataInputStream datainputstream) throws IOException { return null; }
    @Shadow protected static void writeNbtTagCompound(NBTTagCompound nbttagcompound, DataOutputStream dataoutputstream) throws IOException {}

    @Inject(at = @At("RETURN"), method = "<clinit>")
    private static void initHook(CallbackInfo ci) {
        addIdClassMapping(250, true, true, Packet250PluginMessage.class);
    }

    /**
     * @author Fox2Code
     * @reason Standard mixin is too impractical.
     */
    @Overwrite
    public static ItemStack readItemStack(DataInputStream datainputstream) throws IOException {
        ItemStack itemstack = null;
        short itemId = datainputstream.readShort();
        if (itemId >= 0) {
            short translatedItemId = itemId;
            if (datainputstream instanceof NetworkDataInputStream) {
                translatedItemId = GameRegistryClient.itemIdMappingIn[itemId];
            }
            byte byte0 = datainputstream.readByte();
            short word1 = datainputstream.readShort();
            itemstack = new ItemStack(translatedItemId, byte0, word1);
            itemstack.nbtTagCompound = readNbtTagCompound(datainputstream);
            ((NetworkItemStack) (Object) itemstack).setRemoteNetworkId(itemId);
        }

        return itemstack;
    }

    /**
     * @author Fox2Code
     * @reason Standard mixin is too impractical.
     */
    @Overwrite
    public static void writeItemStack(ItemStack itemstack, DataOutputStream dataoutputstream) throws IOException {
        if (itemstack == null) {
            dataoutputstream.writeShort(-1);
        } else {
            int itemId = itemstack.itemID;
            if (itemId >= 0 && dataoutputstream instanceof NetworkDataOutputStream) {
                itemId = ((NetworkItemStack) (Object) itemstack).getRemoteItemId();
            }
            dataoutputstream.writeShort(itemId);
            dataoutputstream.writeByte(itemstack.stackSize);
            dataoutputstream.writeShort(itemstack.getItemDamage());
            writeNbtTagCompound(itemstack.nbtTagCompound, dataoutputstream);
        }
    }
}
