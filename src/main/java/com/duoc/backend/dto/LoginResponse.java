package com.duoc.backend.dto;

public record LoginResponse(String token, String tipo, long expiraEnMs) {
}
