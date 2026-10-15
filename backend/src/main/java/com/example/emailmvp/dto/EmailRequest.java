package com.example.emailmvp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el frontend envía al backend. Las validaciones se ejecutan antes
 * de llamar al Worker.
 */
public record EmailRequest(

        @NotBlank(message = "El destinatario es obligatorio")
        @Email(message = "El destinatario no es un correo válido")
        @Size(max = 254, message = "El destinatario es demasiado largo")
        String to,

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 200, message = "El asunto no debe superar los 200 caracteres")
        String subject,

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 5000, message = "El mensaje no debe superar los 5000 caracteres")
        String message) {
}
