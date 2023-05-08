package notjava.net.http.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import notjava.net.http.HttpClient;
import notjava.net.http.HttpRequest;
import notjava.net.http.HttpResponse;
import notjava.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
        HttpURLConnection httpURLConnection;
        if (this.proxySelector == Builder.NO_PROXY) {
            httpURLConnection = (HttpURLConnection)
                    request.uri().toURL().openConnection(Proxy.NO_PROXY);
        } else {
            httpURLConnection = (HttpURLConnection)
                    request.uri().toURL().openConnection();
        }
        httpURLConnection.setRequestMethod(request.method());
        for (Map.Entry<String, List<String>> entry : request.headers().map().entrySet()) {
            for (String value : entry.getValue()) {
                httpURLConnection.setRequestProperty(entry.getKey(), value);
            }
        }
        Optional<Duration> timeoutOptional = request.timeout();
        if (timeoutOptional.isPresent()) {
            httpURLConnection.setConnectTimeout((int) timeoutOptional.get().toMillis());
        } else if (this.connectTimeout != null) {
            httpURLConnection.setConnectTimeout((int) this.connectTimeout.toMillis());
        }
        httpURLConnection.setDoInput(true);
        Optional<HttpRequest.BodyPublisher> bodyPublisherOptional = request.bodyPublisher();
        HttpRequest.BodyPublisher bodyPublisher;
        if (bodyPublisherOptional.isPresent()) {
            bodyPublisher = bodyPublisherOptional.get();
            if (bodyPublisher instanceof ByteArrayBodyPublisherImpl) {
                httpURLConnection.setDoOutput(true);
                ((ByteArrayBodyPublisherImpl) bodyPublisher)
                        .implDirectWrite(httpURLConnection.getOutputStream());
            }
        }
        HttpResponseImpl<T> httpResponse = new HttpResponseImpl<>(
                httpURLConnection, httpURLConnection.getResponseCode());
        httpResponse.implRead(httpURLConnection.getInputStream(),
                responseBodyHandler);
        return httpResponse;
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
