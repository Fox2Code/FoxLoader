package notjava.net.http;

import notjava.net.http.impl.NullBodySubscriberImpl;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import notjava.util.concurrent.Flow;

/**
 * Partial Implementation For pre Java11 JVMs
 */
public interface HttpResponse<T> {
    int statusCode();

    HttpHeaders headers();

    T body();

    interface ResponseInfo {
        int statusCode();
    }

    @FunctionalInterface
    interface BodyHandler<T> {
        BodySubscriber<T> apply(ResponseInfo responseInfo);

    }

    class BodyHandlers {
        public static BodyHandler<Void> discarding() {
            return (responseInfo) -> BodySubscribers.discarding();
        }

        public static <U> BodyHandler<U> replacing(U value) {
            return (responseInfo) -> BodySubscribers.replacing(value);
        }
    }

    interface BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>> {
        CompletionStage<T> getBody();
    }

    class BodySubscribers {
        private BodySubscribers() {}

        public static <U> BodySubscriber<U> replacing(U value) {
            return new NullBodySubscriberImpl<>(value);
        }

        public static BodySubscriber<Void> discarding() {
            return new NullBodySubscriberImpl<>(null);
        }
    }
}
