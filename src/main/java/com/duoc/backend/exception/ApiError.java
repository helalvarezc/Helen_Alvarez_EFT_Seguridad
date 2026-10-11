package com.duoc.backend.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiError(int status, String error, String mensaje, Instant timestamp) {

    public static ApiError de(HttpStatus status, String mensaje) {
        return new ApiError(status.value(), status.getReasonPhrase(), mensaje, Instant.now());
    }
}
