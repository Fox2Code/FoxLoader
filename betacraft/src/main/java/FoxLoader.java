import com.fox2code.foxloader.launcher.ClientMain;
import org.betacraft.Wrapper;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class FoxLoader extends Wrapper {
    static {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.out.println("FoxLoader BetaCraft wrapper loaded!");
    }

    public static void main(String[] args) throws Exception {
        ClientMain.main(args);
    }

    private static boolean checkAllowDiscordRpc(String mainFolder) {
        // Let allow mods to disable BetaCraft RPC
        return !new File(mainFolder + File.separator + "config" +
                File.separator + "no-discord-rpc").exists();
    }

    public FoxLoader(String user, String ver_prefix, String version, String sessionid, String mainFolder, Integer height, Integer width, Boolean RPC, String launchMethod, String server, String mppass, String uuid, String USR, String VER, Image img, ArrayList<?> addons) {
        super(user, ver_prefix, version, sessionid, mainFolder, height, width,
                RPC && checkAllowDiscordRpc(mainFolder),
                launchMethod, server, mppass, uuid, USR, VER, img, addons);
    }

    public FoxLoader(String user, String ver_prefix, String version, String sessionid, String mainFolder, Integer height, Integer width, Boolean RPC, String launchMethod, String server, String mppass, String USR, String VER, Image img, ArrayList<?> addons) {
        super(user, ver_prefix, version, sessionid, mainFolder, height, width,
                RPC && checkAllowDiscordRpc(mainFolder),
                launchMethod, server, mppass, USR, VER, img, addons);
    }

    @Override
    public void play() {
        this.loadJars();
        boolean hasDiscordRPC = false;
        try {
            hasDiscordRPC = this.discord;
        } catch (Throwable ignored) {}
        try {
            Class.forName("com.fox2code.foxloader.launcher.ClientMain", true, classLoader)
                    .getDeclaredMethod("mainBetaCraft", Map.class, String.class, boolean.class)
                    .invoke(null, this.params, this.mainFolder, hasDiscordRPC);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadMainClass(URL[] url) {
        PrintStream outOrig = System.out;
        PrintStream errOrig = System.err;
        // Silence a normal thing to happen on launch.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        System.setErr(printStream);
        System.setOut(printStream);
        try {
            super.loadMainClass(url);
        } catch (RuntimeException e) {
            try {
                errOrig.write(byteArrayOutputStream.toByteArray());
            } catch (IOException ignored) {}
            System.setOut(outOrig);
            System.setErr(errOrig);
        } finally {
            if (System.out == printStream)
                System.setOut(outOrig);
            if (System.err == printStream)
                System.setErr(errOrig);
        }
    }
}
