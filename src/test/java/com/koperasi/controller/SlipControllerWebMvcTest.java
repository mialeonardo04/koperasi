package com.koperasi.controller;

import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import com.koperasi.service.SlipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SlipController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SlipControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SlipService slipService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void slipSetoran_returnsPdfAttachment() throws Exception {
        when(jwtUtil.extractEmailFromHeader(anyString())).thenReturn("member@koperasi.id");
        when(userRepository.findByEmail("member@koperasi.id"))
                .thenReturn(Optional.of(User.builder().id(1L).build()));

        when(slipService.generateSlipSetoran(10L)).thenReturn(new byte[]{1, 2, 3});

        var auth = new UsernamePasswordAuthenticationToken(
                "member",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        mockMvc.perform(get("/slip/setoran/10")
                        .with(authentication(auth))
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"slip-setoran-10.pdf\""));
    }
}

