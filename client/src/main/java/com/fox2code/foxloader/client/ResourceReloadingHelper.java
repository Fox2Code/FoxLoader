package com.fox2code.foxloader.client;

public class ResourceReloadingHelper {
    private static boolean hasResourceError;

    public static boolean hasResourceError() {
        return hasResourceError;
    }

    public static void notifyResourceError() {
        hasResourceError = true;
    }

    public static class Internal {
        public static void markResourceReloadStart() {
            hasResourceError = false;
        }
    }
}
