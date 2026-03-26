package com.koperasi.service;

import com.koperasi.entity.AuditLog;
import com.koperasi.entity.User;
import com.koperasi.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    @Test
    void log_savesAuditLogWithFallbackRequestDataWhenNoRequest() {
        User user = User.builder()
                .id(1L)
                .email("admin@koperasi.id")
                .namaLengkap("Admin")
                .build();

        auditLogService.log(user, "LOGIN", "users", "1", null, "ok");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertEquals("LOGIN", saved.getAction());
        assertEquals("users", saved.getEntityName());
        assertEquals("1", saved.getEntityId());
        assertEquals("N/A", saved.getIpAddress());
        assertEquals("N/A", saved.getUserAgent());
        assertNotNull(saved.getUser());
        assertEquals("admin@koperasi.id", saved.getUser().getEmail());
    }

    @Test
    void log_whenUserNull_usesSecurityContextUser() {
        try {
            User principal = User.builder()
                    .id(2L)
                    .email("principal@email.com")
                    .namaLengkap("Principal")
                    .build();

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(principal);
            SecurityContextHolder.getContext().setAuthentication(auth);

            auditLogService.log(null, "ACTION", "entity", "123", null, "new");

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();
            assertNotNull(saved.getUser());
            assertEquals("principal@email.com", saved.getUser().getEmail());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void log_readsIpAndUserAgentFromRequest_andPrefersXForwardedFor() {
        try {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.1");
            when(request.getHeader("User-Agent")).thenReturn("JUnit");

            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            User user = User.builder()
                    .id(1L)
                    .email("admin@koperasi.id")
                    .namaLengkap("Admin")
                    .build();

            auditLogService.log(user, "LOGIN", "users", "1", null, "ok");

            verify(auditLogRepository).save(auditLogCaptor.capture());
            AuditLog saved = auditLogCaptor.getValue();
            assertEquals("203.0.113.10", saved.getIpAddress());
            assertEquals("JUnit", saved.getUserAgent());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}

