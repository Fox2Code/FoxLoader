package com.fox2code.foxloader.launcher;

import javax.net.ssl.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;

final class LetsEncryptHelper {
    private static final String prefix = "/assets/foxloader/certificates/";
    private static final String[] certificates = new String[]{"jitpack-io-chain.pem",
            "lets_encrypt_x1_cross_signed.pem", "lets_encrypt_x2_cross_signed.pem",
            "lets_encrypt_x3_cross_signed.pem", "lets_encrypt_x4_cross_signed.pem"};

    public static void installCertificates() throws Exception {
        System.out.println("Using let's encrypt certificates");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        X509TrustManager letsEncryptTrustManager = makeLetsEncryptTrustManager();
        tmf.init((KeyStore) null);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] systemTrustManagers = tmf.getTrustManagers();
        X509TrustManager systemTrustManager;
        if (systemTrustManagers.length != 1 || !(systemTrustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" +
                    Arrays.toString(systemTrustManagers));
        }
        systemTrustManager = (X509TrustManager) systemTrustManagers[0];
        sslContext.init(null, new TrustManager[]{
                new X509DualTrustManager(letsEncryptTrustManager, systemTrustManager)
        }, null);
        Field field = SSLSocketFactory.class.getDeclaredField("theFactory");
        field.setAccessible(true);
        field.set(null, sslContext.getSocketFactory());
    }

    public static X509TrustManager makeLetsEncryptTrustManager()
            throws GeneralSecurityException, IOException {
        LinkedList<Certificate> certificates = new LinkedList<>();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        for (String certificate : LetsEncryptHelper.certificates) {
            certificates.addAll(certificateFactory.generateCertificates(Objects.requireNonNull(
                    LetsEncryptHelper.class.getResourceAsStream(prefix + certificate), certificate)));
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }

        return (X509TrustManager) trustManagers[0];
    }

    public static class X509DualTrustManager implements X509TrustManager {
        private final X509TrustManager letsEncrypt;
        private final X509TrustManager system;
        private final X509Certificate[] certificates;

        public X509DualTrustManager(X509TrustManager letsEncrypt, X509TrustManager system) {
            this.letsEncrypt = letsEncrypt;
            this.system = system;
            HashSet<X509Certificate> certificates = new HashSet<>();
            certificates.addAll(Arrays.asList(this.letsEncrypt.getAcceptedIssuers()));
            certificates.addAll(Arrays.asList(this.system.getAcceptedIssuers()));
            this.certificates = certificates.toArray(new X509Certificate[0]);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                this.letsEncrypt.checkClientTrusted(chain, authType);
            } catch (CertificateException | SecurityException e) {
                this.system.checkClientTrusted(chain, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                this.letsEncrypt.checkServerTrusted(chain, authType);
                System.out.println("checkServerTrusted let's encrypt trust!");
            } catch (CertificateException | SecurityException e) {
                this.system.checkServerTrusted(chain, authType);
                System.out.println("checkServerTrusted system trust!");
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return this.certificates;
        }
    }
}
