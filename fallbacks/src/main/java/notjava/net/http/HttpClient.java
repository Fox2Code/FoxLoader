package notjava.net.http;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import notjava.net.http.impl.HttpClientBuilderImpl;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Partial Implementation For pre Java11 JVMs
 */
public abstract class HttpClient {
    protected HttpClient() {}

    public static HttpClient newHttpClient() {
        return newBuilder().build();
    }

    public static Builder newBuilder() {
        return new HttpClientBuilderImpl();
    }

    public interface Builder {
        ProxySelector NO_PROXY = new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Collections.singletonList(Proxy.NO_PROXY);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                if (uri == null || sa == null || ioe == null) {
                    throw new IllegalArgumentException("Arguments can't be null.");
                }
            }
        };

        Builder connectTimeout(Duration duration);

        Builder proxy(ProxySelector proxySelector);

        HttpClient build();
    }

    public abstract Optional<Duration> connectTimeout();

    public abstract Optional<ProxySelector> proxy();

    public abstract <T> HttpResponse<T> send(
            HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException;

    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler);

    public WebSocket.Builder newWebSocketBuilder() {
        throw new UnsupportedOperationException();
    }
}
