package notjava.net.http.impl;

import java.io.IOException;
import java.io.OutputStream;
import notjava.net.http.HttpRequest;
import java.nio.ByteBuffer;
import notjava.util.concurrent.Flow;

public class ByteArrayBodyPublisherImpl implements HttpRequest.BodyPublisher {
    private final byte[] data;
    private final int off, len;

    public ByteArrayBodyPublisherImpl(byte[] data) {
        this(data, 0, data.length);
    }

    public ByteArrayBodyPublisherImpl(byte[] data, int off, int len) {
        if (off < 0 || (off + len) > data.length) {
            throw new IndexOutOfBoundsException();
        }
        this.data = data;
        this.off = off;
        this.len = len;
    }

    @Override
    public long contentLength() {
        return this.len;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override public void request(long n) {}
            @Override public void cancel() {}
        });
        subscriber.onNext(ByteBuffer.wrap(this.data, this.off, this.len));
        subscriber.onComplete();
    }

    public void implDirectWrite(OutputStream outputStream) throws IOException {
        outputStream.write(this.data, this.off, this.len);
    }
}
