package com.fox2code.foxloader.event.client;

import com.fox2code.foxevents.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ITexturePack;

public final class TexturesRefreshedEvent extends Event {
    public static final TexturesRefreshedEvent INSTANCE = new TexturesRefreshedEvent();

    private TexturesRefreshedEvent() {}

    public ITexturePack getActiveTexturePack() {
        return Minecraft.theMinecraft.texturePackList.getSelectedTexturePack();
    }
}
