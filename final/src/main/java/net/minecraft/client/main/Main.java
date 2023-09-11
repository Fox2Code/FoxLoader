package net.minecraft.client.main;

import com.fox2code.foxloader.launcher.ClientMain;
import net.minecraft.client.Minecraft;

/**
 * Should never be loaded in memory, if it is loaded, it means something wrong happened
 * <br/>
 * Use {@link ClientMain} instead to start the game.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Minecraft.main(args);
    }
}
