package com.cineconecta.cuentas.dto;

public record LoginResponse(String token, String role, Integer accountId, String fullName) {
}
