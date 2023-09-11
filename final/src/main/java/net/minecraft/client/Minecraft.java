package net.minecraft.client;

import com.fox2code.foxloader.launcher.ClientMain;
import com.fox2code.foxloader.launcher.FoxLauncher;

/**
 * Should never be loaded in memory, if it is loaded, it means something wrong happened
 * <br/>
 * Use {@link ClientMain} instead to start the game.
 */
public class Minecraft {
    public static void main(String[] args) throws ReflectiveOperationException {
        if (FoxLauncher.foxLoaderFile.getParentFile().getName().equals("bin")) {
            new MinecraftApplet(); // <- Trigger unrecoverable popup
        } else {
            FoxLauncher.markWronglyInstalled(); // This is bad!
            ClientMain.main(args);
        }
    }
}
