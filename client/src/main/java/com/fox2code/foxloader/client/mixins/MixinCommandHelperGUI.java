package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.mitask.utils.CommandHelperGUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CommandHelperGUI.class)
public class MixinCommandHelperGUI {
    @Shadow static List<String> commandsNames;

    @Inject(method = {"setSPCommands", "setMPCommands"}, at = @At("RETURN"))
    private static void onUpdateCommandList(String message, CallbackInfo ci) {
        for (String key : CommandCompat.clientCommands.keySet()) {
            if (!commandsNames.contains(key)) {
                commandsNames.add(key);
            }
        }
    }
}
