package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.ClientCommandWrapper;
import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.mitask.PlayerCommandHandler;
import net.minecraft.mitask.command.Command;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(PlayerCommandHandler.class)
public class MixinPlayerCommandHandler {
    @Shadow public static ArrayList<Command> commands;

    @Inject(method = "registerCommands",at = @At("RETURN"))
    public void onRegisterCommands(Minecraft mc, CallbackInfo ci) {
        for (CommandCompat commandCompat : CommandCompat.commands.values()) {
            commands.add(new ClientCommandWrapper(commandCompat));
        }
    }
}
