package br.com.notes.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.notes.models.dto.DocumentDTO;
import br.com.notes.models.dto.DocumentInsertOrUpdateDTO;
import br.com.notes.services.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    private static final String API_V1_DOCUMENTS = "/api/v1/documents";
    private static final String OWNER = "qa.admin@workbox.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private DocumentService documentService;

    private RequestPostProcessor auth() {
        return opaqueToken()
                .attributes(attrs -> attrs.put("sub", OWNER))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private DocumentDTO dto(final String title) {
        return new DocumentDTO(UUID.randomUUID().toString(), title, "Conteúdo de teste", List.of("estudo"), Instant.now(), Instant.now());
    }

    @Test
    void search_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_DOCUMENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    void search_withAuth_returnsPage() throws Exception {
        final var dto = dto("Anotações de Java");
        final Page<DocumentDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(documentService.search(eq(OWNER), any())).thenReturn(page);

        mockMvc.perform(get(API_V1_DOCUMENTS).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Anotações de Java"));
    }

    @Test
    void findById_withAuth_returnsDocument() throws Exception {
        final var dto = dto("Anotações de Spring");
        when(documentService.findById(dto.id(), OWNER)).thenReturn(dto);

        mockMvc.perform(get(API_V1_DOCUMENTS + "/" + dto.id()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Anotações de Spring"));
    }

    @Test
    void save_withValidBody_returnsCreated() throws Exception {
        final var insertDto = new DocumentInsertOrUpdateDTO("Anotações de Redis", "TTL, rate limit, cache", List.of("estudo"));
        final var saved = dto("Anotações de Redis");
        when(documentService.save(any(), eq(OWNER))).thenReturn(saved);

        mockMvc.perform(post(API_V1_DOCUMENTS)
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(insertDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(API_V1_DOCUMENTS + "/" + saved.id())))
                .andExpect(jsonPath("$.title").value("Anotações de Redis"));
    }

    @Test
    void save_withBlankTitle_returnsBadRequest() throws Exception {
        final var invalid = new DocumentInsertOrUpdateDTO("", "conteúdo", List.of());

        mockMvc.perform(post(API_V1_DOCUMENTS)
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_withAuth_returnsNoContent() throws Exception {
        mockMvc.perform(delete(API_V1_DOCUMENTS + "/" + UUID.randomUUID()).with(auth()))
                .andExpect(status().isNoContent());
    }
}
