package com.koperasi.controller;

import com.koperasi.dto.PinjamanDto;
import com.koperasi.entity.User;
import com.koperasi.service.KelompokService;
import com.koperasi.service.PinjamanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PinjamanController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class PinjamanControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PinjamanService pinjamanService;

    @MockitoBean
    private KelompokService kelompokService;

    @Test
    void getRiwayat_returnsOk() throws Exception {
        User principal = User.builder().id(1L).role(User.Role.MEMBER).build();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        Page<PinjamanDto.PinjamanResponse> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(pinjamanService.getRiwayatPinjaman(any(Long.class), any())).thenReturn(page);

        mockMvc.perform(get("/pinjaman/riwayat")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}

