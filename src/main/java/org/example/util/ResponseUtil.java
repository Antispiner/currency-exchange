package org.example.util;

import java.util.Map;

public class ResponseUtil {
    public static Map<String, String> errorResponse(String message) {
        return Map.of("error", message);
    }
}
