package br.com.notes.models.dto;

import br.com.notes.models.entities.Document;
import java.time.Instant;
import java.util.List;

public record DocumentDTO(
        String id,
        String title,
        String content,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentDTO from(final Document document) {
        return new DocumentDTO(document.getId(), document.getTitle(), document.getContent(),
                document.getTags(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
