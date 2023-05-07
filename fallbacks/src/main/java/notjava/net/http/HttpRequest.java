package notjava.net.http;

import java.net.URI;
import notjava.net.http.impl.ByteArrayBodyPublisherImpl;
import notjava.net.http.impl.HttpRequestBuilderImpl;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import notjava.util.concurrent.Flow;

/**
 * Partial Implementation For pre Java11 JVMs
 */
public abstract class HttpRequest {
    protected HttpRequest() {}

    public interface Builder {
        Builder uri(URI uri);

        Builder header(String name, String value);

        Builder timeout(Duration duration);

        Builder setHeader(String name, String value);

        Builder GET();

        Builder POST(BodyPublisher bodyPublisher);

        Builder PUT(BodyPublisher bodyPublisher);

        Builder DELETE();

        HttpRequest build();
    }

    public static HttpRequest.Builder newBuilder(URI uri) {
        return new HttpRequestBuilderImpl().uri(uri);
    }

    public static HttpRequest.Builder newBuilder() {
        return new HttpRequestBuilderImpl();
    }

    public abstract Optional<BodyPublisher> bodyPublisher();

    public abstract String method();

    public abstract URI uri();

    public abstract HttpHeaders headers();

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof HttpRequest))
            return false;
        HttpRequest that = (HttpRequest)obj;
        if (that == this) return true;
        return that.method().equals(this.method()) &&
                that.uri().equals(this.uri()) &&
                that.headers().equals(this.headers());
    }

    @Override
    public final int hashCode() {
        return method().hashCode()
                + uri().hashCode()
                + headers().hashCode();
    }

    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {
        long contentLength();
    }

    public static class BodyPublishers {
        private BodyPublishers() {}
        public static BodyPublisher ofString(String body) {
            return ofByteArray(body.getBytes(StandardCharsets.UTF_8));
        }

        public static BodyPublisher ofString(String s, Charset charset) {
            return ofByteArray(s.getBytes(charset));
        }

        public static BodyPublisher ofByteArray(byte[] buf) {
            return new ByteArrayBodyPublisherImpl(buf);
        }

        public static BodyPublisher ofByteArray(byte[] buf, int off, int len) {
            return new ByteArrayBodyPublisherImpl(buf, off, len);
        }

        public static BodyPublisher noBody() {
            return null;
        }
    }
}
