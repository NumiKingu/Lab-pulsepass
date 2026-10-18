package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pulsepass.support.AbstractSecuredWebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
class ArtistControllerTest extends AbstractSecuredWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @Test
    @DisplayName("GET /api/artists/{id} -> 200 cuando existe")
    void findById_existente_retorna200() throws Exception {
        when(artistService.findById(1L)).thenReturn(artist(1L, "Solar Beat"));

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    @Test
    @DisplayName("GET /api/artists/{id} -> 404 cuando no existe")
    void findById_inexistente_retorna404() throws Exception {
        when(artistService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test
    @DisplayName("GET /api/artists/{id} -> 400 cuando el id no es numerico")
    void findById_idInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/artists/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.id").exists());

        verify(artistService, never()).findById(anyLong());
    }

    @Test
    @DisplayName("GET /api/artists/by-stage-name?stageName=... -> 200")
    void findByStageName_retorna200() throws Exception {
        when(artistService.findByStageName("Neon Waves")).thenReturn(artist(2L, "Neon Waves"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Neon Waves"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Neon Waves"));

        verify(artistService).findByStageName("Neon Waves");
    }

    @Test
    @DisplayName("GET /api/artists/by-stage-name -> 404 cuando no existe")
    void findByStageName_inexistente_retorna404() throws Exception {
        when(artistService.findByStageName("Nadie"))
                .thenThrow(new ResourceNotFoundException("Artist not found: Nadie"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Nadie"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/artists/active -> 200")
    void findActiveArtists_retorna200() throws Exception {
        when(artistService.findActiveArtists())
                .thenReturn(List.of(artist(1L, "Solar Beat"), artist(2L, "Neon Waves")));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"));

        verify(artistService).findActiveArtists();
    }

    private ArtistResponse artist(Long id, String stageName) {
        return new ArtistResponse(id, stageName, "Colombia", "Electronic", true);
    }
}
