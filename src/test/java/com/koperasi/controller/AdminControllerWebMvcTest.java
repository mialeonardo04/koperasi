package com.koperasi.controller;

import com.koperasi.entity.AuditLog;
import com.koperasi.entity.User;
import com.koperasi.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class AdminControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private com.koperasi.service.UserManagementService userManagementService;

    @MockBean
    private com.koperasi.service.SimpananService simpananService;

    @MockBean
    private com.koperasi.service.PinjamanService pinjamanService;

    @MockBean
    private com.koperasi.service.ReportService reportService;

    @MockBean
    private com.koperasi.service.TransaksiPendingService transaksiPendingService;

    @MockBean
    private com.koperasi.service.KelompokService kelompokService;

    @Test
    void getAuditLogs_mapsUserToDto() throws Exception {
        User user = User.builder()
                .id(1L)
                .namaLengkap("Administrator Koperasi")
                .email("admin@koperasi.id")
                .build();

        AuditLog log = AuditLog.builder()
                .id(10L)
                .action("LOGIN")
                .entityName("users")
                .entityId("1")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .user(user)
                .build();

        when(auditLogRepository.findAllWithUser(PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 50), 1));

        var auth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/admin/audit-logs")
                        .param("page", "0")
                        .param("size", "50")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.data.content[0].user.email").value("admin@koperasi.id"));
    }
}
