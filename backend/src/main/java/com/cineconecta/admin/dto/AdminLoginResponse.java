package com.cineconecta.admin.dto;

public record AdminLoginResponse(String token, String role, String fullName) {
}
