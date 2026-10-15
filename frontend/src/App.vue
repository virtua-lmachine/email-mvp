<script setup>
import { reactive, ref } from 'vue';

const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(
  /\/$/,
  '',
);

const MAX_SUBJECT_LENGTH = 200;
const MAX_MESSAGE_LENGTH = 5000;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const form = reactive({
  to: '',
  subject: '',
  message: '',
});

const fieldErrors = reactive({
  to: '',
  subject: '',
  message: '',
});

const loading = ref(false);
const result = ref(null);

function clearFieldErrors() {
  fieldErrors.to = '';
  fieldErrors.subject = '';
  fieldErrors.message = '';
}

function validate() {
  clearFieldErrors();

  const to = form.to.trim();
  if (!to) {
    fieldErrors.to = 'El destinatario es obligatorio.';
  } else if (!EMAIL_REGEX.test(to)) {
    fieldErrors.to = 'Escribe un correo válido.';
  }

  if (!form.subject.trim()) {
    fieldErrors.subject = 'El asunto es obligatorio.';
  } else if (form.subject.length > MAX_SUBJECT_LENGTH) {
    fieldErrors.subject = `Máximo ${MAX_SUBJECT_LENGTH} caracteres.`;
  }

  if (!form.message.trim()) {
    fieldErrors.message = 'El mensaje es obligatorio.';
  } else if (form.message.length > MAX_MESSAGE_LENGTH) {
    fieldErrors.message = `Máximo ${MAX_MESSAGE_LENGTH} caracteres.`;
  }

  return !fieldErrors.to && !fieldErrors.subject && !fieldErrors.message;
}

function applyServerErrors(errors) {
  if (!errors) return;
  fieldErrors.to = errors.to || '';
  fieldErrors.subject = errors.subject || '';
  fieldErrors.message = errors.message || '';
}

async function onSubmit() {
  if (loading.value) return;
  result.value = null;
  if (!validate()) return;

  loading.value = true;
  try {
    const response = await fetch(`${API_URL}/api/emails`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        to: form.to.trim(),
        subject: form.subject.trim(),
        message: form.message,
      }),
    });

    let data = null;
    try {
      data = await response.json();
    } catch {
      data = null;
    }

    if (response.ok && data?.ok) {
      result.value = {
        type: 'success',
        message: data.message || 'Correo enviado correctamente.',
      };
      form.to = '';
      form.subject = '';
      form.message = '';
      clearFieldErrors();
    } else {
      applyServerErrors(data?.errors);
      result.value = {
        type: 'error',
        message: data?.message || `Ocurrió un error (HTTP ${response.status}).`,
      };
    }
  } catch {
    result.value = {
      type: 'error',
      message: 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.',
    };
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="card">
    <header class="card__header">
      <h1>Enviar correo</h1>
      <p class="subtitle">Vue.js → Spring Boot → Cloudflare Worker → SendGrid</p>
    </header>

    <form class="form" novalidate @submit.prevent="onSubmit">
      <div class="field">
        <label for="to">Destinatario</label>
        <input
          id="to"
          v-model="form.to"
          type="email"
          autocomplete="email"
          placeholder="destinatario@ejemplo.com"
          :disabled="loading"
          :aria-invalid="Boolean(fieldErrors.to)"
        />
        <p v-if="fieldErrors.to" class="field__error">{{ fieldErrors.to }}</p>
      </div>

      <div class="field">
        <label for="subject">Asunto</label>
        <input
          id="subject"
          v-model="form.subject"
          type="text"
          :maxlength="MAX_SUBJECT_LENGTH"
          placeholder="Asunto del correo"
          :disabled="loading"
          :aria-invalid="Boolean(fieldErrors.subject)"
        />
        <p v-if="fieldErrors.subject" class="field__error">{{ fieldErrors.subject }}</p>
      </div>

      <div class="field">
        <label for="message">Mensaje</label>
        <textarea
          id="message"
          v-model="form.message"
          rows="6"
          :maxlength="MAX_MESSAGE_LENGTH"
          placeholder="Hola, este es un correo de prueba."
          :disabled="loading"
          :aria-invalid="Boolean(fieldErrors.message)"
        ></textarea>
        <div class="field__meta">
          <p v-if="fieldErrors.message" class="field__error">{{ fieldErrors.message }}</p>
          <span class="counter">{{ form.message.length }}/{{ MAX_MESSAGE_LENGTH }}</span>
        </div>
      </div>

      <button type="submit" class="button" :disabled="loading">
        <span v-if="loading" class="spinner" aria-hidden="true"></span>
        {{ loading ? 'Enviando…' : 'Enviar correo' }}
      </button>

      <p v-if="result" class="alert" :class="`alert--${result.type}`" role="status">
        {{ result.message }}
      </p>
    </form>
  </main>
</template>

<style scoped>
.card {
  background: #ffffff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
}

.card__header h1 {
  margin: 0;
  font-size: 1.6rem;
}

.subtitle {
  margin: 6px 0 24px;
  font-size: 0.82rem;
  color: #64748b;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 0.9rem;
  font-weight: 600;
}

.field input,
.field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font: inherit;
  resize: vertical;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.field input:focus,
.field textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.field input[aria-invalid='true'],
.field textarea[aria-invalid='true'] {
  border-color: #dc2626;
}

.field__error {
  margin: 0;
  font-size: 0.8rem;
  color: #dc2626;
}

.field__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.counter {
  margin-left: auto;
  font-size: 0.75rem;
  color: #94a3b8;
}

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 16px;
  border: none;
  border-radius: 10px;
  background: #4f46e5;
  color: #ffffff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.button:hover:not(:disabled) {
  background: #4338ca;
}

.button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.alert {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 0.88rem;
}

.alert--success {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.alert--error {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}
</style>
