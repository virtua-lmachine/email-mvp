package com.example.emailmvp.client;

/**
 * Se lanza cuando no es posible obtener una respuesta interpretable del Worker
 * (timeout, conexión rechazada o respuesta inesperada).
 */
public class WorkerUnavailableException extends RuntimeException {

    public WorkerUnavailableException(String message) {
        super(message);
    }
}
