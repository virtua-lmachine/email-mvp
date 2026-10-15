/**
 * Cloudflare Worker: única responsabilidad -> recibir la solicitud del backend
 * de Spring Boot y solicitar el envío del correo a la API de SendGrid.
 *
 * No guarda estado, no tiene base de datos y no registra el contenido del correo.
 */

const SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";
const MAX_SUBJECT_LENGTH = 200;
const MAX_MESSAGE_LENGTH = 5000;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function json(data, status, cors) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      ...cors,
    },
  });
}

function corsHeaders(origin) {
  return {
    "access-control-allow-origin": origin,
    "access-control-allow-methods": "POST, OPTIONS",
    "access-control-allow-headers": "content-type, x-shared-secret",
    "access-control-max-age": "86400",
  };
}

export default {
  async fetch(request, env) {
    const cors = corsHeaders(env.ALLOWED_ORIGIN || "*");

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }

    if (request.method !== "POST") {
      return json({ ok: false, message: "Método no permitido" }, 405, cors);
    }

    // 1. Autenticación: secreto compartido entre Spring Boot y el Worker.
    if (!env.WORKER_SHARED_SECRET) {
      return json(
        { ok: false, message: "El Worker no tiene configurado WORKER_SHARED_SECRET" },
        500,
        cors,
      );
    }
    if (request.headers.get("x-shared-secret") !== env.WORKER_SHARED_SECRET) {
      return json({ ok: false, message: "No autorizado" }, 401, cors);
    }

    // 2. Parseo del cuerpo JSON.
    let body;
    try {
      body = await request.json();
    } catch {
      return json({ ok: false, message: "Cuerpo JSON inválido" }, 400, cors);
    }

    const to = typeof body?.to === "string" ? body.to.trim() : "";
    const subject = typeof body?.subject === "string" ? body.subject.trim() : "";
    const message = typeof body?.message === "string" ? body.message : "";

    // 3. Validación básica (la validación principal vive en Spring Boot).
    if (!EMAIL_REGEX.test(to)) {
      return json({ ok: false, message: "Destinatario inválido" }, 422, cors);
    }
    if (subject.length === 0 || subject.length > MAX_SUBJECT_LENGTH) {
      return json({ ok: false, message: "Asunto inválido" }, 422, cors);
    }
    if (message.trim().length === 0 || message.length > MAX_MESSAGE_LENGTH) {
      return json({ ok: false, message: "Mensaje inválido" }, 422, cors);
    }

    if (!env.SENDGRID_API_KEY) {
      return json(
        { ok: false, message: "El Worker no tiene configurado SENDGRID_API_KEY" },
        500,
        cors,
      );
    }

    // 4. Llamada a SendGrid.
    let sgResponse;
    try {
      sgResponse = await fetch(SENDGRID_URL, {
        method: "POST",
        headers: {
          authorization: `Bearer ${env.SENDGRID_API_KEY}`,
          "content-type": "application/json",
        },
        body: JSON.stringify({
          personalizations: [{ to: [{ email: to }] }],
          from: {
            email: env.FROM_EMAIL || "no-reply@example.com",
            name: env.FROM_NAME || "Email MVP",
          },
          subject,
          content: [{ type: "text/plain", value: message }],
        }),
      });
    } catch {
      return json({ ok: false, message: "No se pudo contactar a SendGrid" }, 502, cors);
    }

    // SendGrid responde 202 Accepted cuando la solicitud queda en cola.
    if (sgResponse.status === 202) {
      return json(
        { ok: true, message: "SendGrid aceptó la solicitud de envío" },
        200,
        cors,
      );
    }

    // No registramos el contenido del correo ni credenciales. Solo un extracto del error.
    let details = "";
    try {
      details = (await sgResponse.text()).slice(0, 500);
    } catch {
      details = "";
    }
    return json(
      {
        ok: false,
        message: `SendGrid rechazó la solicitud (${sgResponse.status})`,
        details,
      },
      502,
      cors,
    );
  },
};
