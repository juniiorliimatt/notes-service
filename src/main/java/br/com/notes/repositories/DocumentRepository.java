package br.com.notes.repositories;

import br.com.notes.models.entities.Document;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentRepository extends MongoRepository<Document, String> {

    Page<Document> findByOwnerUsername(String ownerUsername, Pageable pageable);

    Optional<Document> findByIdAndOwnerUsername(String id, String ownerUsername);
}
