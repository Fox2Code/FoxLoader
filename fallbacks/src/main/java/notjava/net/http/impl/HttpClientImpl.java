package notjava.net.http.impl;

import java.io.IOException;
import java.net.ProxySelector;
import notjava.net.http.HttpClient;
import notjava.net.http.HttpRequest;
import notjava.net.http.HttpResponse;
import notjava.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HttpClientImpl extends HttpClient {
    private final Duration connectTimeout;
    private final ProxySelector proxySelector;

    public HttpClientImpl(Duration connectTimeout, ProxySelector proxySelector) {
        this.connectTimeout = connectTimeout;
        this.proxySelector = proxySelector;
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(this.connectTimeout);
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.ofNullable(this.proxySelector);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.send(request, responseBodyHandler);
            } catch (Throwable t) {
                FallbackHttpUtils.sneakyThrow(t);
                return null;
            }
        });
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        return new WebSocketBuilderImpl(this);
    }
}
