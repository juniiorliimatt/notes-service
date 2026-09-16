package br.com.notes.models.entities;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Conteúdo colado manualmente pelo usuário (migração de notas antigas de outro app) —
 * sem schema fixo por natureza, daqui vem a escolha de MongoDB em vez de Postgres: título
 * e conteúdo livre, tags opcionais, sem relacionamento com outras entidades do monorepo.
 *
 * @author Junior Lima - oojuniiin@outlook.com
 * @since 16/09/2026
 */
@Getter
@Setter
@Builder
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {

    @Id
    private String id;

    private String title;

    private String content;

    @Builder.Default
    private List<String> tags = List.of();

    @Field("owner_username")
    private String ownerUsername;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
