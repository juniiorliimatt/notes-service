package br.com.notes.exceptions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.notes.controllers.DocumentController;
import br.com.notes.services.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Erros de roteamento HTTP são do client (404/405/415) — nunca podem cair no catch-all e virar 500. */
@ActiveProfiles("test")
@WebMvcTest(DocumentController.class)
class RestExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    private static RequestPostProcessor user() {
        return opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("rota inexistente responde 404 em problem+json, não 500")
    void rotaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/v1/nao-existe").with(user()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Resource not found"));
    }

    @Test
    @DisplayName("método HTTP não suportado responde 405 com o cabeçalho Allow")
    void metodoNaoSuportado_retorna405() throws Exception {
        mockMvc.perform(patch("/api/v1/documents").with(user()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.detail").value("HTTP method not supported on this route"));
    }

    @Test
    @DisplayName("Content-Type não suportado responde 415")
    void tipoDeConteudoNaoSuportado_retorna415() throws Exception {
        mockMvc.perform(post("/api/v1/documents").with(user()).contentType(MediaType.TEXT_PLAIN).content("oi"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Unsupported content type"));
    }
}
