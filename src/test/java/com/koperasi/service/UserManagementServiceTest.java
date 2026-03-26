package com.koperasi.service;

import com.koperasi.dto.AuthDto;
import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TelegramService telegramService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserManagementService userManagementService;

    @Test
    void updateStatusMember_updatesUserAndWritesAuditLog() {
        User user = User.builder()
                .id(1L)
                .nomorAnggota("MBR-0001")
                .namaLengkap("Budi")
                .email("budi@email.com")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthDto.UserInfo res = userManagementService.updateStatusMember(1L, User.StatusAnggota.SUSPEND);

        assertEquals("SUSPEND", res.getStatus());

        verify(auditLogService).log(
                isNull(),
                eq("UPDATE_USER_STATUS"),
                eq("users"),
                eq("1"),
                eq("status: AKTIF"),
                eq("status: SUSPEND")
        );
    }

    @Test
    void adminCreateUser_savesUserAndWritesAuditLog() {
        AuthDto.AdminCreateUserRequest req = new AuthDto.AdminCreateUserRequest();
        req.setNamaLengkap("Siti");
        req.setEmail("siti@email.com");
        req.setPassword("password123");
        req.setNoTelepon("628123");
        req.setAlamat("Alamat");
        req.setRole(User.Role.MEMBER);
        req.setTelegramChatId("123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });

        AuthDto.UserInfo res = userManagementService.adminCreateUser(req);

        assertEquals(9L, res.getId());
        assertEquals("siti@email.com", res.getEmail());

        verify(auditLogService).log(
                isNull(),
                eq("CREATE_USER"),
                eq("users"),
                eq("9"),
                isNull(),
                eq("email: siti@email.com, role: MEMBER")
        );

        verify(telegramService).send(eq("123"), anyString());
    }

    @Test
    void updateProfileMember_updatesFields() {
        User user = User.builder()
                .id(1L)
                .nomorAnggota("MBR-0001")
                .namaLengkap("Budi")
                .email("budi@email.com")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthDto.UserInfo res = userManagementService.updateProfileMember(1L, "Budi Baru", "62812", "Alamat Baru");

        assertEquals("Budi Baru", res.getNamaLengkap());
        assertEquals("budi@email.com", res.getEmail());
    }

    @Test
    void updateRole_updatesRole() {
        User user = User.builder()
                .id(1L)
                .nomorAnggota("MBR-0001")
                .namaLengkap("Budi")
                .email("budi@email.com")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthDto.UserInfo res = userManagementService.updateRole(1L, User.Role.ADMIN);
        assertEquals("ADMIN", res.getRole());
    }

    @Test
    void updateTelegramChatId_updatesField() {
        User user = User.builder()
                .id(1L)
                .nomorAnggota("MBR-0001")
                .namaLengkap("Budi")
                .email("budi@email.com")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthDto.UserInfo res = userManagementService.updateTelegramChatId(1L, "999");
        assertEquals("999", res.getTelegramChatId());
    }

    @Test
    void getAllMembers_routesToSearchQueryWhenSearchProvided() {
        User user = User.builder()
                .id(1L)
                .nomorAnggota("MBR-0001")
                .namaLengkap("Budi")
                .email("budi@email.com")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .totalSimpanan(java.math.BigDecimal.ZERO)
                .build();

        PageRequest pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAllMembersBySearch(eq("budi"), eq(pageable))).thenReturn(page);

        Page<AuthDto.UserInfo> res = userManagementService.getAllMembers(" budi ", pageable);
        assertEquals(1, res.getTotalElements());
        verify(userRepository).findAllMembersBySearch(eq("budi"), eq(pageable));
    }
}
