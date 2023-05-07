package notjava.util.concurrent;

/**
 * Partial Implementation For pre Java9 JVMs
 */
public class Flow {
    private Flow() {}

    @FunctionalInterface
    public interface Publisher<T> {
        void subscribe(Subscriber<? super T> subscriber);
    }

    public interface Subscriber<T> {
        void onSubscribe(Subscription subscription);

        void onNext(T item);

        void onError(Throwable throwable);

        void onComplete();
    }

    public interface Subscription {
        void request(long n);

        void cancel();
    }

    public interface Processor<T,R> extends Subscriber<T>, Publisher<R> {}

    public static int defaultBufferSize() {
        return 256;
    }
}
