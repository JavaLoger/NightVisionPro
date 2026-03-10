package org.loger.signalr;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class HubErrorParser {
    public static final String AUTH_FAILED = "AUTH_FAILED";
    private static final Gson GSON = new Gson();
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String INVALID_DATA = "INVALID_DATA";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
    public static final String STATS_EXPIRED = "STATS_EXPIRED";
    public static final String STATS_REQUIRED = "STATS_REQUIRED";

    public static HubError parse(String exceptionMessage) {
        if (exceptionMessage == null || exceptionMessage.isEmpty()) {
            return new HubError(INTERNAL_ERROR, "Unknown error");
        }
        String json = extractJson(exceptionMessage);
        if (json == null) {
            return new HubError(INTERNAL_ERROR, exceptionMessage);
        }
        try {
            JsonObject obj = (JsonObject) GSON.fromJson(json, JsonObject.class);
            String code = INTERNAL_ERROR;
            if (obj.has("code")) {
                code = obj.get("code").getAsString();
            } else if (obj.has("Code")) {
                code = obj.get("Code").getAsString();
            }
            String message = exceptionMessage;
            if (obj.has("message")) {
                message = obj.get("message").getAsString();
            } else if (obj.has("Message")) {
                message = obj.get("Message").getAsString();
            }
            return new HubError(code, message);
        } catch (JsonSyntaxException e) {
            return new HubError(INTERNAL_ERROR, exceptionMessage);
        }
    }

    private static String extractJson(String text) {
        int start = text.indexOf(123);
        int end = text.lastIndexOf(125);
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    public static boolean isRetryable(String code) {
        return STATS_REQUIRED.equals(code) || STATS_EXPIRED.equals(code) || NOT_AUTHENTICATED.equals(code);
    }

    public static boolean requiresReportStats(String code) {
        return STATS_REQUIRED.equals(code) || STATS_EXPIRED.equals(code);
    }

    public static boolean requiresReconnection(String code) {
        return NOT_AUTHENTICATED.equals(code);
    }

    public static class HubError {
        private final String code;
        private final String message;

        public HubError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return this.code;
        }

        public String getMessage() {
            return this.message;
        }

        public String toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("code", this.code);
            obj.addProperty("message", this.message);
            return HubErrorParser.GSON.toJson((JsonElement) obj);
        }

        public static HubError fromJson(String json) {
            return HubErrorParser.parse(json);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HubError other = (HubError) obj;
            if (this.code.equals(other.code) && this.message.equals(other.message)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (this.code.hashCode() * 31) + this.message.hashCode();
        }

        public String toString() {
            return "HubError{code='" + this.code + "', message='" + this.message + "'}";
        }
    }
}
