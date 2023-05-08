package notjava.net.http.impl;

import notjava.net.http.HttpHeaders;
import notjava.net.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HttpResponseImpl<T> implements HttpResponse<T>, HttpResponse.ResponseInfo {
    private final HttpHeaders httpHeaders;
    private final int statusCode;
    private CompletableFuture<T> body;

    public HttpResponseImpl(HttpURLConnection httpURLConnection, int statusCode) {
        this.httpHeaders = HttpHeaders.of(httpURLConnection
                .getHeaderFields(), (k, v) -> true);
        this.statusCode = statusCode;
    }

    @Override
    public int statusCode() {
        return this.statusCode;
    }

    @Override
    public HttpHeaders headers() {
        return this.httpHeaders;
    }

    @Override
    public T body() {
        return body.join();
    }

    public void implRead(InputStream inputStream,
                         HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException {
        BodySubscriber<T> bodySubscriber = bodyHandler.apply(this);
        byte[] buffer = new byte[2048];
        List<ByteBuffer> byteBufferList =
                Collections.singletonList(
                        ByteBuffer.wrap(buffer));
        int len;
        try {
            while ((len = inputStream.read(buffer)) > 0) {
                if (len == buffer.length) {
                    bodySubscriber.onNext(byteBufferList);
                } else {
                    bodySubscriber.onNext(Collections.singletonList(
                            ByteBuffer.wrap(buffer, 0, len)));
                }
            }
        } catch (IOException ioe) {
            bodySubscriber.onError(ioe);
            throw ioe;
        }
        this.body = bodySubscriber.getBody().toCompletableFuture();
    }
}
