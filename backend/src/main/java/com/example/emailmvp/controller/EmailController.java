package com.example.emailmvp.controller;

import com.example.emailmvp.client.WorkerClient;
import com.example.emailmvp.client.WorkerResult;
import com.example.emailmvp.dto.EmailRequest;
import com.example.emailmvp.dto.EmailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Único endpoint público de negocio: solicitar el envío de un correo.
 */
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final WorkerClient workerClient;

    public EmailController(WorkerClient workerClient) {
        this.workerClient = workerClient;
    }

    @PostMapping
    public ResponseEntity<EmailResponse> send(@Valid @RequestBody EmailRequest request) {
        WorkerResult result = workerClient.send(request);

        HttpStatus status = result.ok() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status)
                .body(new EmailResponse(result.ok(), result.message()));
    }
}
