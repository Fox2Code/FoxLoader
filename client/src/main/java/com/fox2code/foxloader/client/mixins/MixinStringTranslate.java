package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientModLoader;
import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.client.gui.StringTranslate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Properties;

@Mixin(StringTranslate.class)
public class MixinStringTranslate {
    @Shadow private static Properties translateTable;

    @Shadow public static String langFile;

    @Inject(method = "reloadKeys", at = @At("RETURN"))
    private static void fillDefaultTranslations(CallbackInfo ci) {
        if (!langFile.equals("en_US")) {
            ClientModLoader.Internal.getTranslationsForLanguage(langFile)
                    .forEach(translateTable::putIfAbsent);
        }
        ModLoader.Internal.fallbackTranslations.forEach(translateTable::putIfAbsent);
    }
}
