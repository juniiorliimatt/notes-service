package br.com.notes.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.notes.exceptions.ResourceNotFoundException;
import br.com.notes.models.dto.DocumentInsertOrUpdateDTO;
import br.com.notes.models.entities.Document;
import br.com.notes.repositories.DocumentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final String OWNER = "alice@example.com";
    private static final String OTHER_OWNER = "bob@example.com";

    @Mock
    private DocumentRepository documentRepository;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository);
    }

    private Document.DocumentBuilder aDocument() {
        return Document.builder().id("64f0a1b2c3d4e5f678901234").title("Título").content("Conteúdo").tags(List.of("tag1")).ownerUsername(OWNER);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("lança ResourceNotFoundException quando o documento não existe")
        void throwsWhenMissing() {
            when(documentRepository.findByIdAndOwnerUsername("missing", OWNER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById("missing", OWNER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("lança ResourceNotFoundException (não 403) quando o documento pertence a outro usuário — não revela existência")
        void throwsWhenOwnedByAnotherUser() {
            final var document = aDocument().build();
            when(documentRepository.findByIdAndOwnerUsername(document.getId(), OTHER_OWNER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(document.getId(), OTHER_OWNER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("devolve o DTO quando o documento pertence ao usuário autenticado")
        void returnsDtoWhenOwned() {
            final var document = aDocument().build();
            when(documentRepository.findByIdAndOwnerUsername(document.getId(), OWNER)).thenReturn(Optional.of(document));

            final var result = service.findById(document.getId(), OWNER);

            assertThat(result.title()).isEqualTo("Título");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("atribui ownerUsername a partir do usuário autenticado, nunca do payload")
        void assignsOwnerFromAuthenticatedUser() {
            final var dto = new DocumentInsertOrUpdateDTO("Novo título", "Novo conteúdo", List.of("estudo"));
            when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            final var result = service.save(dto, OWNER);

            assertThat(result.title()).isEqualTo("Novo título");
            assertThat(result.tags()).containsExactly("estudo");
        }

        @Test
        @DisplayName("tags nulas viram lista vazia, não null")
        void nullTagsBecomeEmptyList() {
            final var dto = new DocumentInsertOrUpdateDTO("Título", "Conteúdo", null);
            when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            final var result = service.save(dto, OWNER);

            assertThat(result.tags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("lança ResourceNotFoundException quando o documento não pertence ao usuário")
        void throwsWhenNotOwned() {
            final var dto = new DocumentInsertOrUpdateDTO("Título", "Conteúdo", List.of());
            when(documentRepository.findByIdAndOwnerUsername("id-1", OTHER_OWNER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update("id-1", dto, OTHER_OWNER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("aplica as mudanças quando o documento pertence ao usuário")
        void appliesChangesWhenOwned() {
            final var document = aDocument().build();
            final var dto = new DocumentInsertOrUpdateDTO("Título atualizado", "Conteúdo atualizado", List.of("nova-tag"));
            when(documentRepository.findByIdAndOwnerUsername(document.getId(), OWNER)).thenReturn(Optional.of(document));
            when(documentRepository.save(document)).thenReturn(document);

            final var result = service.update(document.getId(), dto, OWNER);

            assertThat(result.title()).isEqualTo("Título atualizado");
            assertThat(result.tags()).containsExactly("nova-tag");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("lança ResourceNotFoundException quando o documento não pertence ao usuário")
        void throwsWhenNotOwned() {
            when(documentRepository.findByIdAndOwnerUsername("id-1", OTHER_OWNER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete("id-1", OTHER_OWNER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
