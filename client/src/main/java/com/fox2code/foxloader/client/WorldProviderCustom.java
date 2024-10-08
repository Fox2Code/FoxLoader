package com.fox2code.foxloader.client;

import net.minecraft.src.game.level.*;
import net.minecraft.src.game.level.chunk.ChunkProviderFlatworld;
import net.minecraft.src.game.level.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

public class WorldProviderCustom extends WorldProvider {
    public  static Map<String,WorldProviderCustom> customWorldProviders=new HashMap<>();
    public String wpName;
    public static WorldProvider getProviderForDimensioncustom(String arg0,int i) {

        if (arg0!=null) {
                WorldProvider w=customWorldProviders.get(arg0);
                if(w!=null) return w;
        }

        if (i == -1) {
            return new WorldProviderHell();
        } else {
            return new WorldProviderSurface();
        }
    }



    public static void addprovider(String name, WorldProviderCustom provider){
        provider.wpName=name;
        provider.worldType=3;
        customWorldProviders.put(name, provider);
    }
    @Override
    public IChunkProvider getChunkProvider() {
        if(!worldObj.multiplayerWorld) {
            if (this.worldObj.getWorldInfo().getGenType()!=0&&this.worldObj.getWorldInfo().getGenType()!=-1) {
                return new ChunkProviderFlatworld(this.worldObj, this.worldObj.getRandomSeed(), this.worldObj.getWorldInfo().isMapFeaturesEnabled());
            }
        }return super.getChunkProvider();
    }
}
