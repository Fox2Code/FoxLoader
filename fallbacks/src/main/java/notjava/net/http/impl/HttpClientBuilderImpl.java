package notjava.net.http.impl;

import java.net.ProxySelector;
import notjava.net.http.HttpClient;
import java.time.Duration;

public final class HttpClientBuilderImpl implements HttpClient.Builder {
    private Duration connectTimeout;
    private ProxySelector proxySelector;

    @Override
    public HttpClient.Builder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public HttpClient.Builder proxy(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
        return this;
    }

    @Override
    public HttpClient build() {
        return new HttpClientImpl(this.connectTimeout, this.proxySelector);
    }
}
