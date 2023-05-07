package notjava.net.http;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Partial Implementation For pre Java11 JVMs
 */
public final class HttpHeaders {

    private final Map<String, List<String>> headers;

    private HttpHeaders(Map<String,List<String>> headers) {
        this.headers = headers;
    }

    public Map<String,List<String>> map() {
        return this.headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpHeaders that = (HttpHeaders) o;
        return this.headers.equals(that.headers);
    }

    @Override
    public int hashCode() {
        return this.headers.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + " { " + headers + " }";
    }

    private static final HttpHeaders NO_HEADERS = new HttpHeaders(Collections.emptyMap());

    public static HttpHeaders of(Map<String,List<String>> map,
                                         BiPredicate<String,String> filter) {

        TreeMap<String,List<String>> other = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> notAdded = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ArrayList<String> tempList = new ArrayList<>();
        map.forEach((key, value) -> {
            String headerName = Objects.requireNonNull(key).trim();
            if (headerName.isEmpty()) {
                throw new IllegalArgumentException("empty key");
            }
            List<String> headerValues = Objects.requireNonNull(value);
            headerValues.forEach(headerValue -> {
                headerValue = Objects.requireNonNull(headerValue).trim();
                if (filter.test(headerName, headerValue)) {
                    tempList.add(headerValue);
                }
            });

            if (tempList.isEmpty()) {
                if (other.containsKey(headerName)
                        || notAdded.contains(headerName.toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("duplicate key: " + headerName);
                notAdded.add(headerName.toLowerCase(Locale.ROOT));
            } else if (other.put(headerName, new ArrayList<>(tempList)) != null) {
                throw new IllegalArgumentException("duplicate key: " + headerName);
            }
            tempList.clear();
        });
        return other.isEmpty() ? NO_HEADERS : new HttpHeaders(Collections.unmodifiableMap(other));
    }
}
