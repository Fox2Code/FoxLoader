package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.registry.CommandCompat;
import net.minecraft.src.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(GuiTextField.class)
public class MixinGuiTextField {
    @Redirect(method = "textboxKeyTyped", at = @At(value = "INVOKE",
            target = "Ljava/lang/String;length()I", ordinal = 0))
    public int checkClientCommand(String instance) {
        if (!instance.startsWith("/"))
            return instance.length();
        for (Map.Entry<String, CommandCompat> entry :
                CommandCompat.clientCommands.entrySet()) {
            boolean match = instance.startsWith("/" + entry.getKey() + " ") ||
                    instance.equals("/" + entry.getKey());
            if (!match) {
                for (String alias : entry.getValue().getAliases()) {
                    match = instance.startsWith("/" + alias + " ") ||
                            instance.equals("/" + alias);
                    if (match) break;
                }
            }

            if (match) {
                entry.getValue().onExecute(instance.split(" "),
                        ClientMod.getLocalNetworkPlayer());
                return 0;
            }
        }
        return instance.length();
    }
}
