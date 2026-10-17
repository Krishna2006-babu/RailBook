
package com.railbook.dto;

public record LoginResponse(
        Long userId,
        String name,
        String email,
        String message
) {}
