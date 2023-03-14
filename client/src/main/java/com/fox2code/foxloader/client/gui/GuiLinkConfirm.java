package com.fox2code.foxloader.client.gui;

import com.fox2code.foxloader.launcher.utils.Platform;
import net.minecraft.src.client.gui.*;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;

public class GuiLinkConfirm extends GuiYesNo {
    private final GuiScreen parentScreen;
    private final String link;
    private final String openText;
    private final String copyText;
    private final String cancelText;
    private final String warningText;

    public GuiLinkConfirm(GuiScreen origin, String link) {
        super(origin, StringTranslate.getInstance().translateKey("chat.link.confirm"), link, "", "", -1);
        this.parentScreen = origin;
        this.link = link;
        StringTranslate stringTranslate = StringTranslate.getInstance();
        this.openText = stringTranslate.translateKey("chat.link.open");
        this.copyText = stringTranslate.translateKey("chat.copy");
        this.cancelText = stringTranslate.translateKey("gui.no");
        this.warningText = stringTranslate.translateKey("chat.link.warning");
    }

    @Override
    public void initGui() {
        final int y = this.height / 6 + 96;
        this.controlList.add(new GuiSmallButton(0, this.width / 2 - 50 - 105, y, 100, 20, this.openText));
        this.controlList.add(new GuiSmallButton(1, this.width / 2 - 50, y, 100, 20, this.copyText));
        this.controlList.add(new GuiSmallButton(2, this.width / 2 - 50 + 105, y, 100, 20, this.cancelText));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                this.openLinkInBrowser();
                break;
            case 1:
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(this.link), null);
                break;
            case 2:
        }
        this.mc.displayGuiScreen(this.parentScreen);
    }

    private void openLinkInBrowser() {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create(this.link));
                    return;
                } catch (IOException ignored) {}
            }
        }
        try {
            Runtime.getRuntime().exec(new String[]{Platform.getPlatform().open, this.link});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int var1, int var2, float deltaTicks) {
        super.drawScreen(var1, var2, deltaTicks);
        this.drawCenteredString(this.fontRenderer, this.warningText, this.width / 2, 110, 16764108);
    }
}
