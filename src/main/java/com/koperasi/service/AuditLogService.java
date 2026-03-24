package com.koperasi.service;

import com.koperasi.entity.AuditLog;
import com.koperasi.entity.User;
import com.koperasi.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Mencatat log audit. Jika user null, akan mencoba mengambil dari SecurityContext.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, String action, String entityName, String entityId, String oldValue, String newValue) {
        try {
            if (user == null) {
                user = getCurrentUser();
            }

            HttpServletRequest request = getCurrentRequest();
            String ipAddress = request != null ? getClientIp(request) : "N/A";
            String userAgent = request != null ? request.getHeader("User-Agent") : "N/A";

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} by user {}", action, user != null ? user.getEmail() : "SYSTEM");
        } catch (Exception e) {
            log.error("Gagal menyimpan audit log: {}", e.getMessage());
            // Tidak throw exception agar proses bisnis utama tidak terganggu
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
