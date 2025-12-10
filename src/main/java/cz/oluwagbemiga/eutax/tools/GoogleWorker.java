package cz.oluwagbemiga.eutax.tools;

import java.util.Optional;

public final class GoogleWorker {

    private static volatile String googleServiceAccountJson;

    private GoogleWorker() {
        throw new IllegalStateException("Utility class");
    }

    public static void setGoogleServiceAccountJson(String json) {
        googleServiceAccountJson = json;
    }

    public static Optional<String> getGoogleServiceAccountJson() {
        return Optional.ofNullable(googleServiceAccountJson);
    }

    public static boolean hasGoogleServiceAccountJson() {
        return googleServiceAccountJson != null && !googleServiceAccountJson.isBlank();
    }

    public static void clearGoogleServiceAccountJson() {
        googleServiceAccountJson = null;
    }
}
