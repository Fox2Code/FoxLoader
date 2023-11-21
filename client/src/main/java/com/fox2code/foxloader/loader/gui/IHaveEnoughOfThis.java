package com.fox2code.foxloader.loader.gui;

import com.fox2code.foxloader.network.ChatColors;
import net.minecraft.client.Minecraft;
import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiMainMenu;
import net.minecraft.src.client.gui.GuiScreen;
import net.minecraft.src.client.gui.GuiSmallButton;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

public final class IHaveEnoughOfThis extends GuiScreen {
    private static final long WAIT_TIME = 30_000;
    private static final long rootTimerUnlockMs =
            System.currentTimeMillis() + WAIT_TIME;
    private static int ret = 0;
    private static boolean timerOk;
    private GuiSmallButton continueButton;
    private final long screenTimerUnlockMs;

    public IHaveEnoughOfThis() {
        super();
        this.screenTimerUnlockMs = System.currentTimeMillis() + WAIT_TIME;
    }

    @Override
    public void initGui() {
        this.controlList.add(new GuiSmallButton(0, this.width / 2 - 155,
                this.height / 6 + 116, "Open GitHub"));
        this.controlList.add(this.continueButton =
                new GuiSmallButton(1, this.width / 2 - 155 + 160, this.height / 6 + 116,
                        "Continue (30)"));
        this.continueButton.enabled = false;
    }

    @Override
    public void updateScreen() {
        long timeLeftMs = screenTimerUnlockMs - System.currentTimeMillis();
        if (timeLeftMs < 0) {
            this.continueButton.displayString = "Continue";
            this.continueButton.enabled = true;
            return;
        }
        int sec = (int) (1 + (timeLeftMs / 1000));
        this.continueButton.displayString = "Continue (" + sec + ")";
        this.continueButton.enabled = false;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            Sys.openURL("https://github.com/Fox2Code/FoxLoader");
        } else if (button.id == 1 &&
                this.screenTimerUnlockMs <
                        System.currentTimeMillis()) {
            timerOk = true;
            Minecraft.getInstance().displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    protected void keyTyped(char eventChar, int eventKey) {}

    public void drawScreen(int var1, int var2, float deltaTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer,
                "I have enough of making FoxLoader " + ChatColors.RED + "alone" + ChatColors.RESET +
                        ", I already asked support multiples times already.",
                this.width / 2, 70, 16777215);
        this.drawCenteredString(this.fontRenderer,
                "As I can't take it anymore, I'll keep this unskipable screen until dynamic textures are fixed",
                this.width / 2, 90, 16777215);
        this.drawCenteredString(this.fontRenderer,
                "You can submit a PR at " + ChatColors.AQUA + "https://github.com/Fox2Code/FoxLoader" +
                        ChatColors.RESET + " to help fix dynamic textures.",
                this.width / 2, 110, 16777215);
        this.drawCenteredString(this.fontRenderer,
                "As soon as dynamic textures works on FoxLoader, this screen will be removed.",
                this.width / 2, 130, 16777215);
        super.drawScreen(var1, var2, deltaTicks);
    }

    public static void update() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            GuiScreen screen = minecraft.currentScreen;
            if (timerOk) {
                if (rootTimerUnlockMs < System.currentTimeMillis())
                    return;
                minecraft.renderEngine = null;
                minecraft.renderGlobal = null;
            }
            if (screen == null || screen.getClass() != IHaveEnoughOfThis.class) {
                if (ret++ > 3) minecraft.fontRenderer = null;
                minecraft.displayGuiScreen(new IHaveEnoughOfThis());
            }
        } catch (Throwable t) {
            Display.destroy();
            System.exit(-1);
        }
    }
}
