package notjava.net.http;

import java.io.IOException;

public class WebSocketHandshakeException extends IOException {
    private final transient HttpResponse<?> response;

    public WebSocketHandshakeException(HttpResponse<?> response) {
        this.response = response;
    }

    public HttpResponse<?> getResponse() {
        return this.response;
    }

    @Override
    public WebSocketHandshakeException initCause(Throwable cause) {
        return (WebSocketHandshakeException) super.initCause(cause);
    }
}
