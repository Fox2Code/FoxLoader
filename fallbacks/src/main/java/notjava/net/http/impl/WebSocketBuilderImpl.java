package notjava.net.http.impl;

import java.io.IOError;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import notjava.net.http.HttpRequest;
import notjava.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WebSocketBuilderImpl implements WebSocket.Builder {
    private final HashMap<String, List<String>> headers = new HashMap<>();
    private final HttpClientImpl httpClient;
    private final MessageDigest sha1;
    {
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("Minimum requirements", e);
        }
    }
    private Duration connectTimeout;

    public WebSocketBuilderImpl(HttpClientImpl httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public WebSocket.Builder header(String name, String value) {
        List<String> list = headers.computeIfAbsent(name, k -> new ArrayList<>());
        list.clear();
        list.add(value);
        return this;
    }

    @Override
    public WebSocket.Builder connectTimeout(Duration timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    @Override
    public CompletableFuture<WebSocket> buildAsync(final URI uri,final WebSocket.Listener listener) {
        return CompletableFuture.supplyAsync(() -> {
            URI httpURI = createRequestURI(uri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(httpURI);
            if (this.connectTimeout != null) {
                builder.timeout(this.connectTimeout);
            }
            final String nonce = createNonce();
            HttpURLConnection httpURLConnection;
            try {
                httpURLConnection = (HttpURLConnection) httpURI.toURL().openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Sec-WebSocket-Version", "13");
                httpURLConnection.setRequestProperty("Sec-WebSocket-Key", nonce);
                httpURLConnection.setRequestProperty("Upgrade", "websocket");
                httpURLConnection.setRequestProperty("Connection", "Upgrade");
                if (this.connectTimeout != null) {
                    httpURLConnection.setConnectTimeout(
                            (int) this.connectTimeout.toMillis());
                }
            } catch (Throwable t) {
                FallbackHttpUtils.sneakyThrow(t);
                throw new InternalError(t);
            }

            String x = nonce + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            this.sha1.update(x.getBytes(StandardCharsets.ISO_8859_1));
            String expected = Base64.getEncoder().encodeToString(this.sha1.digest());
            List<String> accept = httpURLConnection.getHeaderFields().get("Sec-WebSocket-Accept");
            if (accept == null || accept.size() != 1 || !expected.equals(accept.get(0).trim())) {
                IOException ioException = new IOException("Bad Sec-WebSocket-Accept");
                FallbackHttpUtils.sneakyThrow(ioException);
                throw new IOError(ioException);
            }
            try {
                return new WebSocketImpl(httpURLConnection, httpURI, "", listener);
            } catch (IOException e) {
                FallbackHttpUtils.sneakyThrow(e);
                throw new IOError(e);
            }
        });
    }

    static URI createRequestURI(URI uri) {
        String s = uri.getScheme();
        assert "ws".equalsIgnoreCase(s) || "wss".equalsIgnoreCase(s);
        String newUri = uri.toString();
        if (s.equalsIgnoreCase("ws")) {
            newUri = "http" + newUri.substring(2);
        }
        else {
            newUri = "https" + newUri.substring(3);
        }
        try {
            return new URI(newUri);
        } catch (URISyntaxException e) {
            // Shouldn't happen: URI invariant
            throw new InternalError(e);
        }
    }

    private static String createNonce() {
        byte[] bytes = new byte[16];
        FallbackHttpUtils.random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
