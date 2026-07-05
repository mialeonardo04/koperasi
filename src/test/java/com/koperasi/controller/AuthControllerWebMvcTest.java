package com.koperasi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.dto.AuthDto;
import com.koperasi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class AuthControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_returnsOkResponse() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("admin@koperasi.id");
        req.setPassword("admin123");

        AuthDto.UserInfo user = AuthDto.UserInfo.builder()
                .id(1L)
                .nomorAnggota("ADM-0001")
                .namaLengkap("Administrator Koperasi")
                .email("admin@koperasi.id")
                .role("ADMIN")
                .status("AKTIF")
                .totalSimpanan(BigDecimal.ZERO)
                .build();

        AuthDto.LoginResponse res = AuthDto.LoginResponse.builder()
                .accessToken("token")
                .expiresIn(86400000L)
                .user(user)
                .build();

        when(authService.login(any(AuthDto.LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login berhasil"))
                .andExpect(jsonPath("$.data.accessToken").value("token"))
                .andExpect(jsonPath("$.data.user.email").value("admin@koperasi.id"));
    }
}

