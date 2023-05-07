package notjava.net.http.impl;

import notjava.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import notjava.util.concurrent.Flow;

public class NullBodySubscriberImpl<T> implements HttpResponse.BodySubscriber<T> {
    private final CompletableFuture<T> body;

    public NullBodySubscriberImpl(T object) {
        this.body = CompletableFuture.completedFuture(object);
    }

    @Override
    public CompletionStage<T> getBody() {
        return this.body;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {}

    @Override
    public void onNext(List<ByteBuffer> item) {}

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}
}
