package com.kitchen.recipe.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        List<String> details,
        Instant timestamp
) {
    public ErrorResponse(String errorCode, String message) {

        this(errorCode, message, List.of(), Instant.now());
    }

    public ErrorResponse(String errorCode, String message, List<String> details) {
        this(errorCode, message, details, Instant.now());
    }
}
