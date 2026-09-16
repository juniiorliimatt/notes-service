package br.com.notes.services;

import br.com.notes.exceptions.ResourceNotFoundException;
import br.com.notes.models.dto.DocumentDTO;
import br.com.notes.models.dto.DocumentInsertOrUpdateDTO;
import br.com.notes.models.entities.Document;
import br.com.notes.repositories.DocumentRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Todo acesso é escopado por {@code ownerUsername} — {@link #findById} devolve
 * {@link ResourceNotFoundException} tanto pra id inexistente quanto pra documento de
 * outro usuário, nunca 403: não revela a existência de um recurso alheio (IDOR, OWASP
 * API1:2023), mesmo padrão do budget-service.
 */
@Service
public class DocumentService {

    private static final String DOCUMENT_NOT_FOUND = "Document not found";

    private final DocumentRepository documentRepository;

    public DocumentService(final DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Page<DocumentDTO> search(final String ownerUsername, final Pageable pageable) {
        return documentRepository.findByOwnerUsername(ownerUsername, pageable).map(DocumentDTO::from);
    }

    public DocumentDTO findById(final String id, final String ownerUsername) {
        return DocumentDTO.from(findEntityById(id, ownerUsername));
    }

    public DocumentDTO save(final DocumentInsertOrUpdateDTO dto, final String ownerUsername) {
        final var document = Document.builder()
                .title(dto.title())
                .content(dto.content())
                .tags(dto.tags() == null ? List.of() : dto.tags())
                .ownerUsername(ownerUsername)
                .build();
        return DocumentDTO.from(documentRepository.save(document));
    }

    public DocumentDTO update(final String id, final DocumentInsertOrUpdateDTO dto, final String ownerUsername) {
        final var document = findEntityById(id, ownerUsername);
        document.setTitle(dto.title());
        document.setContent(dto.content());
        document.setTags(dto.tags() == null ? List.of() : dto.tags());
        return DocumentDTO.from(documentRepository.save(document));
    }

    public void delete(final String id, final String ownerUsername) {
        documentRepository.delete(findEntityById(id, ownerUsername));
    }

    private Document findEntityById(final String id, final String ownerUsername) {
        return documentRepository.findByIdAndOwnerUsername(id, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
    }
}
