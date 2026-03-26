package com.koperasi.controller;

import com.koperasi.dto.SimpananDto;
import com.koperasi.entity.User;
import com.koperasi.service.SimpananService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimpananController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class SimpananControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimpananService simpananService;

    @Test
    void getSaldo_returnsOk() throws Exception {
        User principal = User.builder().id(1L).role(User.Role.MEMBER).build();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        SimpananDto.SaldoResponse saldo = SimpananDto.SaldoResponse.builder()
                .simpananPokok(new BigDecimal("500000"))
                .simpananWajib(new BigDecimal("1500000"))
                .simpananSukarela(new BigDecimal("250000"))
                .totalSimpanan(new BigDecimal("2250000"))
                .build();

        when(simpananService.getSaldo(1L)).thenReturn(saldo);

        mockMvc.perform(get("/simpanan/saldo")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSimpanan").value(2250000));
    }
}
