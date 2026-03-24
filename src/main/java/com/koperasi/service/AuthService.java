package com.koperasi.service;

import com.koperasi.dto.AuthDto;
import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import com.koperasi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    private static final AtomicLong memberCounter = new AtomicLong(0);

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = (User) auth.getPrincipal();
            String token = jwtUtil.generateToken(user);

            // Audit Log Login
            auditLogService.log(user, "LOGIN", "users", user.getId().toString(), null, "User login berhasil");

            return AuthDto.LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(86400000L)
                    .user(mapToUserInfo(user))
                    .build();
        } catch (Exception e) {
            throw new BadCredentialsException("Email atau password salah");
        }
    }

    @Transactional
    public AuthDto.UserInfo register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }

        String nomorAnggota = generateNomorAnggota();

        User user = User.builder()
                .nomorAnggota(nomorAnggota)
                .namaLengkap(request.getNamaLengkap())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .noTelepon(request.getNoTelepon())
                .alamat(request.getAlamat())
                .tanggalGabung(LocalDate.now())
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .totalSimpanan(BigDecimal.ZERO)
                .build();

        user = userRepository.save(user);
        log.info("Member baru terdaftar: {} - {}", nomorAnggota, request.getNamaLengkap());

        // Audit Log Register
        auditLogService.log(user, "REGISTER", "users", user.getId().toString(), null, "Member baru terdaftar mandiri");

        return mapToUserInfo(user);
    }

    @Transactional
    public void changePassword(String email, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        if (!passwordEncoder.matches(request.getPasswordLama(), user.getPassword())) {
            throw new BadCredentialsException("Password lama tidak sesuai");
        }

        user.setPassword(passwordEncoder.encode(request.getPasswordBaru()));
        userRepository.save(user);

        // Audit Log
        auditLogService.log(user, "CHANGE_PASSWORD", "users", user.getId().toString(), null, "Password diperbarui");
    }

    public AuthDto.UserInfo getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
        return mapToUserInfo(user);
    }

    private String generateNomorAnggota() {
        long count = userRepository.count() + 1 + memberCounter.getAndIncrement();
        return String.format("MBR-%04d", count);
    }

    private AuthDto.UserInfo mapToUserInfo(User user) {
        return AuthDto.UserInfo.builder()
                .id(user.getId())
                .nomorAnggota(user.getNomorAnggota())
                .namaLengkap(user.getNamaLengkap())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .totalSimpanan(user.getTotalSimpanan())
                .build();
    }
}