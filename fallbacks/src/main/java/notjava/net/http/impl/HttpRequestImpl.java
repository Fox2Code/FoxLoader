package notjava.net.http.impl;

import java.net.URI;
import notjava.net.http.HttpHeaders;
import notjava.net.http.HttpRequest;
import java.util.Optional;

public class HttpRequestImpl extends HttpRequest {
    private final String method;
    private final URI uri;
    private final HttpHeaders httpHeaders;
    private final HttpRequest.BodyPublisher bodyPublisher;

    public HttpRequestImpl(String method, URI uri, HttpHeaders httpHeaders, BodyPublisher bodyPublisher) {
        this.method = method;
        this.uri = uri;
        this.httpHeaders = httpHeaders;
        this.bodyPublisher = bodyPublisher;
    }

    @Override
    public Optional<BodyPublisher> bodyPublisher() {
        return Optional.ofNullable(this.bodyPublisher);
    }

    @Override
    public String method() {
        return this.method;
    }

    @Override
    public URI uri() {
        return this.uri;
    }

    @Override
    public HttpHeaders headers() {
        return this.httpHeaders;
    }


}
