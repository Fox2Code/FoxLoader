package notjava.net.http.impl;

import java.net.URI;
import notjava.net.http.HttpHeaders;
import notjava.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpRequestBuilderImpl implements HttpRequest.Builder {
    private final HashMap<String, List<String>> headers = new HashMap<>();
    private HttpRequest.BodyPublisher bodyPublisher;
    private Duration timeout;
    private String method;
    private URI uri;

    public HttpRequestBuilderImpl() {
        this.method = "GET";
    }

    @Override
    public HttpRequest.Builder uri(URI uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public HttpRequest.Builder header(String name, String value) {
        List<String> list = headers.computeIfAbsent(name, k -> new ArrayList<>());
        list.clear();
        list.add(value);
        return this;
    }

    @Override
    public HttpRequest.Builder timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    @Override
    public HttpRequest.Builder setHeader(String name, String value) {
        List<String> list = headers.computeIfAbsent(name, k -> new ArrayList<>());
        list.clear();
        list.add(value);
        return this;
    }

    @Override
    public HttpRequest.Builder GET() {
        this.method = "GET";
        this.bodyPublisher = null;
        return this;
    }

    @Override
    public HttpRequest.Builder POST(HttpRequest.BodyPublisher bodyPublisher) {
        this.method = "POST";
        this.bodyPublisher = bodyPublisher;
        return this;
    }

    @Override
    public HttpRequest.Builder PUT(HttpRequest.BodyPublisher bodyPublisher) {
        this.method = "PUT";
        this.bodyPublisher = bodyPublisher;
        return this;
    }

    @Override
    public HttpRequest.Builder DELETE() {
        this.method = "DELETE";
        this.bodyPublisher = null;
        return this;
    }

    @Override
    public HttpRequest build() {
        return new HttpRequestImpl(this.method, this.timeout, this.uri,
                HttpHeaders.of(this.headers, (k, v) -> true), this.bodyPublisher);
    }
}
