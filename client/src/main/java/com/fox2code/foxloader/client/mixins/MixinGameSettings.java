package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.KeyBindingAPI;
import net.minecraft.src.client.EnumOptions;
import net.minecraft.src.client.GameSettings;
import net.minecraft.src.client.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public class MixinGameSettings {
    @Shadow public KeyBinding[] keyBindings;

    @Redirect(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at =
    @At(value = "INVOKE", target = "Lnet/minecraft/src/client/GameSettings;loadOptions()V"))
    public void onLoadOptionInit(GameSettings instance) {
        this.keyBindings = KeyBindingAPI.Internal.inject(this.keyBindings);
        instance.loadOptions();
    }

    @Inject(method = "<init>()V", at = @At("RETURN"))
    public void onOptionInit(CallbackInfo ci) {
        this.keyBindings = KeyBindingAPI.Internal.inject(this.keyBindings);
    }

    // Temporary hotfix.
    @Shadow public int limitFramerate;

    @Inject(method = "getKeyBinding", at = @At("HEAD"))
    public void hotfix_onGetKeyBinding(EnumOptions option, CallbackInfoReturnable<String> cir) {
        if (option == EnumOptions.FRAMERATE_LIMIT && this.limitFramerate > 2 &&
                !EnumOptions.FRAMERATE_LIMIT.getEnumFloat()) {
            this.limitFramerate = 0;
        }
    }
}
