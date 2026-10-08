package br.com.pabelis.quickclock.adapters.in.web.shared;

import java.util.List;

public record ApiErrorResponse(
    String message,
    List<String> details
) {

    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(message, List.of());
    }

    public static ApiErrorResponse of(String message, List<String> details) {
        return new ApiErrorResponse(message, details);
    }
}
