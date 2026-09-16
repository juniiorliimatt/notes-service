package br.com.notes.controllers;

import br.com.notes.models.dto.DocumentDTO;
import br.com.notes.models.dto.DocumentInsertOrUpdateDTO;
import br.com.notes.services.DocumentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(final DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> search(@PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) final Pageable pageable,
                                                      final Authentication authentication) {
        return ResponseEntity.ok(documentService.search(authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> findById(@PathVariable final String id, final Authentication authentication) {
        return ResponseEntity.ok(documentService.findById(id, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<DocumentDTO> save(@RequestBody @Valid final DocumentInsertOrUpdateDTO dto, final UriComponentsBuilder uriBuilder,
                                             final Authentication authentication) {
        final var saved = documentService.save(dto, authentication.getName());
        final URI uri = uriBuilder.path("/api/v1/documents/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> update(@PathVariable final String id, @RequestBody @Valid final DocumentInsertOrUpdateDTO dto,
                                               final Authentication authentication) {
        return ResponseEntity.ok(documentService.update(id, dto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final String id, final Authentication authentication) {
        documentService.delete(id, authentication.getName());
    }
}
