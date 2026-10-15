package com.example.emailmvp.client;

import com.example.emailmvp.config.WorkerProperties;
import com.example.emailmvp.dto.EmailRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envía la solicitud al Cloudflare Worker mediante HTTPS.
 *
 * Usamos onStatus(...) para NO lanzar excepción por códigos 4xx/5xx: el Worker
 * siempre responde JSON con {ok, message}, y preferimos leer ese cuerpo tal cual.
 * Solo los fallos de transporte (sin respuesta) se convierten en excepción.
 */
@Component
public class WorkerClient {

    private final RestClient client;
    private final WorkerProperties properties;

    public WorkerClient(RestClient workerRestClient, WorkerProperties properties) {
        this.client = workerRestClient;
        this.properties = properties;
    }

    public WorkerResult send(EmailRequest request) {
        try {
            WorkerResult result = client.post()
                    .uri("")
                    .header("x-shared-secret", properties.sharedSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> {
                        // No lanzar: leemos el cuerpo JSON del Worker en cualquier caso.
                    })
                    .body(WorkerResult.class);

            if (result == null) {
                throw new WorkerUnavailableException("El Worker devolvió una respuesta vacía.");
            }
            return result;
        } catch (ResourceAccessException ex) {
            throw new WorkerUnavailableException(
                    "No se pudo contactar al Worker (timeout o conexión rechazada).");
        } catch (RestClientException ex) {
            throw new WorkerUnavailableException("Respuesta inesperada del Worker.");
        }
    }
}
