package com.koperasi.service;

import com.koperasi.dto.AuthDto;
import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import com.koperasi.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_returnsTokenAndWritesAuditLog() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("admin@koperasi.id");
        req.setPassword("admin123");

        User user = User.builder()
                .id(1L)
                .email("admin@koperasi.id")
                .namaLengkap("Admin")
                .role(User.Role.ADMIN)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(user);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        AuthDto.LoginResponse res = authService.login(req);

        assertEquals("jwt-token", res.getAccessToken());
        assertNotNull(res.getUser());
        assertEquals("admin@koperasi.id", res.getUser().getEmail());

        verify(auditLogService).log(user, "LOGIN", "users", "1", null, "User login berhasil");
    }

    @Test
    void register_savesUserAndWritesAuditLog() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setNamaLengkap("Budi");
        req.setEmail("budi@email.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthDto.UserInfo info = authService.register(req);

        assertEquals(10L, info.getId());
        assertEquals("budi@email.com", info.getEmail());
        assertEquals("MEMBER", info.getRole());

        verify(auditLogService).log(any(User.class), eq("REGISTER"), eq("users"), eq("10"), isNull(), eq("Member baru terdaftar mandiri"));
    }

    @Test
    void changePassword_updatesPasswordAndWritesAuditLog() {
        AuthDto.ChangePasswordRequest req = new AuthDto.ChangePasswordRequest();
        req.setPasswordLama("old");
        req.setPasswordBaru("newpass123");

        User user = User.builder()
                .id(5L)
                .email("member@email.com")
                .password("encodedOld")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findByEmail("member@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("encodedNew");

        authService.changePassword("member@email.com", req);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        assertEquals("encodedNew", savedUserCaptor.getValue().getPassword());

        verify(auditLogService).log(user, "CHANGE_PASSWORD", "users", "5", null, "Password diperbarui");
    }

    @Test
    void login_whenAuthenticationFails_throwsBadCredentials() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("admin@koperasi.id");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("fail"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> authService.login(req));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void register_whenEmailExists_throws() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setNamaLengkap("Budi");
        req.setEmail("budi@email.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void changePassword_whenOldPasswordNotMatch_throws() {
        AuthDto.ChangePasswordRequest req = new AuthDto.ChangePasswordRequest();
        req.setPasswordLama("old");
        req.setPasswordBaru("newpass123");

        User user = User.builder()
                .id(5L)
                .email("member@email.com")
                .password("encodedOld")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findByEmail("member@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encodedOld")).thenReturn(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.changePassword("member@email.com", req));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
}
