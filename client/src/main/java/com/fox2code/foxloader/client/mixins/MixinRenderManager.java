package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.loader.Mod;
import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.client.renderer.entity.Render;
import net.minecraft.src.client.renderer.entity.RenderManager;
import net.minecraft.src.game.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(RenderManager.class)
public class MixinRenderManager {
	@Shadow private Map<Class<?>, Render> entityRenderMap;

	@Inject(method = "<init>", at = @At(value = "INVOKE", ordinal = 45, shift = At.Shift.AFTER,
			target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private void onInitLastPut(CallbackInfo ci) {
		Map<Class<? extends Entity>, Render> renderMap = new HashMap<>();

		for (ModContainer container : ModLoader.getModContainers()) {
			Mod clientMod = container.getClientMod();
			if (clientMod instanceof ClientMod) {
				((ClientMod) clientMod).registerRenderers(renderMap);
			}
		}

		entityRenderMap.putAll(renderMap);
	}
}
