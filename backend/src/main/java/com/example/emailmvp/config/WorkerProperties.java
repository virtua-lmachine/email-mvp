package com.example.emailmvp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la conexión con el Cloudflare Worker.
 * Se enlaza con las propiedades que empiezan por "app.worker".
 */
@ConfigurationProperties(prefix = "app.worker")
public record WorkerProperties(
        String url,
        String sharedSecret,
        int connectTimeoutMs,
        int readTimeoutMs) {
}
