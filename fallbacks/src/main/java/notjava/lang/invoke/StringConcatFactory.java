package notjava.lang.invoke;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class StringConcatFactory {
    private static final char TAG_ARG = '\u0001';
    private static final char TAG_CONST = '\u0002';

    public static CallSite makeConcat(MethodHandles.Lookup lookup,
                                      String name,
                                      MethodType concatType) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static CallSite makeConcatWithConstants(
            MethodHandles.Lookup lookup, String name, MethodType concatType,
            String recipe, Object... constants) throws NoSuchMethodException, IllegalAccessException {
        String hardcodedMethodName;
        switch (recipe) {
            case "https://\u0001/":
                hardcodedMethodName = "httpsUrlFormat";
                break;
            case "wss://\u0001/":
                hardcodedMethodName = "wssUrlFormat";
                break;
            case "Request failed: \u0001":
                hardcodedMethodName = "requestFailedFormat";
                break;
            case "Location header not returned: \u0001":
                hardcodedMethodName = "locationHeaderNotReturnedFormat";
                break;
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }
        return new ConstantCallSite(lookup.findStatic(HardCodedImpls.class, hardcodedMethodName, concatType));
    }

    public static class HardCodedImpls {
        public static String httpsUrlFormat(String text) {
            return String.format("https://%s/", text);
        }

        public static String wssUrlFormat(String text) {
            return String.format("wss://%s/", text);
        }

        public static String requestFailedFormat(String text) {
            return String.format("Request failed: %s", text);
        }

        public static String locationHeaderNotReturnedFormat(String text) {
            return String.format("Location header not returned: %s", text);
        }
    }
}
