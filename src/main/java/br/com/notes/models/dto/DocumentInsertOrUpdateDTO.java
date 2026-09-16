package br.com.notes.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Sem {@code id}/{@code ownerUsername} — nunca aceitos do payload do client, mesmo padrão
 * anti mass-assignment (BOLA, OWASP API3:2023) usado no workbox-api/budget-service:
 * {@code id} é gerado pelo Mongo, {@code ownerUsername} vem sempre do
 * {@code Authentication} da requisição autenticada.
 */
public record DocumentInsertOrUpdateDTO(
        @NotBlank @Size(max = 250) String title,
        @NotBlank String content,
        List<String> tags) {
}
