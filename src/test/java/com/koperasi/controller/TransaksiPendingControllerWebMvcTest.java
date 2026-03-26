package com.koperasi.controller;

import com.koperasi.dto.TransaksiPendingDto;
import com.koperasi.entity.User;
import com.koperasi.service.PinjamanService;
import com.koperasi.service.TransaksiPendingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransaksiPendingController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class TransaksiPendingControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransaksiPendingService service;

    @MockBean
    private PinjamanService pinjamanService;

    @Test
    void ajukanSetor_returnsOk() throws Exception {
        User principal = User.builder().id(1L).role(User.Role.MEMBER).build();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        TransaksiPendingDto.TransaksiPendingResponse res = TransaksiPendingDto.TransaksiPendingResponse.builder()
                .id(10L)
                .status("PENDING")
                .build();

        when(service.ajukanSetor(eq(1L), any(TransaksiPendingDto.AjukanSetorRequest.class))).thenReturn(res);

        mockMvc.perform(post("/pengajuan/setor")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jenisSimpanan\":\"WAJIB\",\"jumlah\":100000,\"keterangan\":\"Test\",\"buktiBayar\":\"2026/03/x.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
