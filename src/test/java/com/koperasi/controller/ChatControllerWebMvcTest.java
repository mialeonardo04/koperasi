package com.koperasi.controller;

import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import com.koperasi.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class ChatControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void chat_whenPesanBlank_returnsBadRequest() throws Exception {
        when(jwtUtil.extractEmailFromHeader(anyString())).thenReturn("member@koperasi.id");
        when(userRepository.findByEmail("member@koperasi.id"))
                .thenReturn(Optional.of(User.builder().id(1L).build()));

        var auth = new UsernamePasswordAuthenticationToken(
                "member",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        mockMvc.perform(post("/chat/message")
                        .with(authentication(auth))
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesan\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void resetHistory_returnsOk() throws Exception {
        when(jwtUtil.extractEmailFromHeader(anyString())).thenReturn("member@koperasi.id");
        when(userRepository.findByEmail("member@koperasi.id"))
                .thenReturn(Optional.of(User.builder().id(1L).build()));

        var auth = new UsernamePasswordAuthenticationToken(
                "member",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        mockMvc.perform(delete("/chat/reset")
                        .with(authentication(auth))
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
