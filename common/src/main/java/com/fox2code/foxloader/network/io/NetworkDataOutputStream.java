package com.fox2code.foxloader.network.io;

import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * Indicate to FoxLoader that this DataOutputStream is made for Network usage.
 */
public class NetworkDataOutputStream extends DataOutputStream {
    public NetworkDataOutputStream(OutputStream out) {
        super(out);
    }
}
