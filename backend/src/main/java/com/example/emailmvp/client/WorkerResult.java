package com.example.emailmvp.client;

/**
 * Respuesta que devuelve el Worker. `ok` indica si SendGrid aceptó la solicitud.
 */
public record WorkerResult(boolean ok, String message, String details) {
}
