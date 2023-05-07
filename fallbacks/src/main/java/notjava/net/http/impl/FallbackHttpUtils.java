package notjava.net.http.impl;

import java.security.SecureRandom;

final class FallbackHttpUtils {
    static final SecureRandom random = new SecureRandom();
    static final byte[] NULL_BUFFER = new byte[0];

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
