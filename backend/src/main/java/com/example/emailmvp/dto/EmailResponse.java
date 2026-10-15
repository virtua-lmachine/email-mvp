package com.example.emailmvp.dto;

/**
 * Respuesta sencilla y consistente que el backend devuelve al frontend.
 */
public record EmailResponse(boolean ok, String message) {
}
