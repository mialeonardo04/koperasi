package com.koperasi.controller;

import com.koperasi.dto.KelompokDto;
import com.koperasi.entity.User;
import com.koperasi.service.KelompokService;
import com.koperasi.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KelompokController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class KelompokControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KelompokService kelompokService;

    @MockitoBean
    private UserManagementService userManagementService;

    @Test
    void getKelompokSaya_returnsOk() throws Exception {
        User principal = User.builder().id(1L).role(User.Role.MEMBER).build();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        KelompokDto.KelompokResponse res = KelompokDto.KelompokResponse.builder()
                .id(10L)
                .namaKelompok("Kelompok A")
                .build();

        when(kelompokService.getKelompokSaya(1L)).thenReturn(res);

        mockMvc.perform(get("/kelompok/saya")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.namaKelompok").value("Kelompok A"));
    }
}
