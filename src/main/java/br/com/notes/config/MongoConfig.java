package br.com.notes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Separado de {@code NotesServiceApplication} de propósito: {@code @EnableMongoAuditing}
 * na classe principal quebra {@code @WebMvcTest} ({@code mongoAuditingHandler} tenta
 * resolver {@code mongoMappingContext}, que não existe num slice test que exclui a
 * autoconfiguração do Mongo) — numa {@code @Configuration} à parte, o filtro de tipos do
 * {@code @WebMvcTest} não a carrega.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
