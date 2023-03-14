package com.fox2code.foxloader.server.mixins;

import com.fox2code.foxloader.loader.ModLoader;
import net.minecraft.src.server.playergui.StringTranslate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Properties;

@Mixin(StringTranslate.class)
public class MixinStringTranslate {
    @Redirect(method = {"translateKey", "translateKeyFormat"}, at =
    @At(value = "INVOKE", target = "Ljava/util/Properties;getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
    public String redirect(Properties instance, String key, String def) {
        String translation = instance.getProperty(key);
        if (translation != null) return translation;
        return ModLoader.Internal.fallbackTranslations.getProperty(key, def);
    }
}
