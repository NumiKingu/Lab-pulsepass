package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String VALID_BODY = """
            {
              "username": "andrea",
              "email": "andrea@pulsepass.com",
              "firstName": "Andrea",
              "lastName": "Perez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "1998-05-10"
            }
            """;


    @Test
    @DisplayName("POST /api/users -> 201 con request valido")
    void register_valido_retorna201() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(user());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@pulsepass.com"))
                .andExpect(jsonPath("$.active").value(true));

        ArgumentCaptor<RegisterUserRequest> captor = ArgumentCaptor.forClass(RegisterUserRequest.class);
        verify(userService).register(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("andrea");
        assertThat(captor.getValue().birthDate()).isEqualTo(LocalDate.of(1998, 5, 10));
    }


    @Test
    @DisplayName("POST /api/users -> 400 con email invalido y sin llamar al Service")
    void register_emailInvalido_retorna400() throws Exception {
        String invalid = """
                {
                  "username": "andrea",
                  "email": "no-es-un-email",
                  "firstName": "Andrea",
                  "lastName": "Perez",
                  "birthDate": "1998-05-10"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email format is invalid"));

        verify(userService, never()).register(any());
    }


    @Test
    @DisplayName("POST /api/users -> 400 con campos obligatorios vacios")
    void register_camposObligatorios_retorna400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"));

        verify(userService, never()).register(any());
    }


    @Test
    @DisplayName("POST /api/users -> 409 si el username o email ya existen")
    void register_duplicado_retorna409() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"))
                .andExpect(jsonPath("$.message").value(not(containsString("Exception"))));
    }


    @Test
    @DisplayName("GET /api/users/by-email?email=... -> 200")
    void findByEmail_retorna200() throws Exception {
        when(userService.findByEmail("andrea@pulsepass.com")).thenReturn(user());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@pulsepass.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@pulsepass.com"));

        verify(userService).findByEmail("andrea@pulsepass.com");
    }


    @Test
    @DisplayName("GET /api/users/by-email -> 404 cuando no existe")
    void findByEmail_inexistente_retorna404() throws Exception {
        when(userService.findByEmail("x@x.com"))
                .thenThrow(new ResourceNotFoundException("User not found: x@x.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "x@x.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: x@x.com"));
    }


    @Test
    @DisplayName("GET /api/users/by-email sin parametro -> 400")
    void findByEmail_sinParametro_retorna400() throws Exception {
        mockMvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter"))
                .andExpect(jsonPath("$.details.email").value("Parameter is required"));

        verify(userService, never()).findByEmail(anyString());
    }


    @Test
    @DisplayName("GET /api/users/by-username?username=... -> 200")
    void findByUsername_retorna200() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(user());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    private UserResponse user() {
        return new UserResponse(1L, "andrea", "andrea@pulsepass.com", true,
                "Andrea", "Perez", "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10));
    }
}
