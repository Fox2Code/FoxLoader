package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.updater.UpdateManager;
import net.minecraft.src.client.PanelCrashReport;
import net.minecraft.src.client.UnexpectedThrowable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.*;
import java.awt.*;

@Mixin(PanelCrashReport.class)
public class MixinPanelCrashReport extends Panel {
    @Unique JButton updateFoxLoader;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onCrash(UnexpectedThrowable var1, CallbackInfo ci) {
        this.updateFoxLoader = new JButton("Update FoxLoader");
        Dimension dimension = new Dimension(200, 50);
        this.updateFoxLoader.setMaximumSize(dimension);
        JPanel jPanel = new JPanel();
        jPanel.setMinimumSize(dimension);
        jPanel.setPreferredSize(dimension);
        jPanel.setBackground(null);
        jPanel.setLayout(new GridBagLayout());
        jPanel.add(this.updateFoxLoader);
        this.add(jPanel, BorderLayout.SOUTH);
        UpdateManager.getInstance().bindButtonToFoxLoaderUpdate(this.updateFoxLoader);
    }
}
