package org.betacraft;

import java.applet.Applet;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wrapper extends Applet {
    public final Map<String, String> params = new HashMap<>();
    public String mainFolder;
    public URLClassLoader classLoader;
    public boolean discord = false;

    public Wrapper(String user, String ver_prefix, String version, String sessionid,
                   String mainFolder, Integer height, Integer width, Boolean RPC, String launchMethod, String server,
                   String mppass, String uuid, String USR, String VER, Image img, ArrayList<?> addons) {
        this(user, ver_prefix, version, sessionid, mainFolder, height, width, RPC, launchMethod, server, mppass, USR, VER, img, addons);
    }

    public Wrapper(String user, String ver_prefix, String version, String sessionid,
                   String mainFolder, Integer height, Integer width, Boolean RPC, String launchMethod, String server,
                   String mppass, String USR, String VER, Image img, ArrayList<?> addons) {
        params.put("username", user);
        params.put("sessionid", sessionid);
        params.put("haspaid", "true");

        if (server != null && server.contains(":")) {
            params.put("server", server.split(":")[0]);
            params.put("port", server.split(":")[1]);
        }

        this.play();
    }

    public void play() {

    }

    public void loadMainClass(URL[] url) {}

    public void loadJars() {}

    @Override
    public URL getCodeBase() {
        try {
            return new URL("http://www.minecraft.net/game/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getParameter(final String paramName) {
        System.out.println("Client asked for parameter: " + paramName);
        if (params.containsKey(paramName)) {
            return params.get(paramName);
        }
        return null;
    }
}
