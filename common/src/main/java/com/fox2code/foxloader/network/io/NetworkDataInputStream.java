package com.fox2code.foxloader.network.io;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Indicate to FoxLoader that this DataInputStream is made for Network usage.
 */
public class NetworkDataInputStream extends DataInputStream {
    public NetworkDataInputStream(@NotNull InputStream in) {
        super(in);
    }
}
