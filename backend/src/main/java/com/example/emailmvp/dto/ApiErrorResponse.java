package com.example.emailmvp.dto;

import java.util.Map;

/**
 * Forma común de los errores devueltos por el backend.
 * `errors` solo se rellena en errores de validación (campo -> mensaje).
 */
public record ApiErrorResponse(boolean ok, String message, Map<String, String> errors) {

    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(false, message, null);
    }
}
