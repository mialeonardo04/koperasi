package com.koperasi.service;

import com.koperasi.dto.AuthDto;
import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TelegramService  telegramService;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    public Page<AuthDto.UserInfo> getAllMembers(String search, Pageable pageable) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        Page<User> page = hasSearch
                ? userRepository.findAllMembersBySearch(search.trim(), pageable)
                : userRepository.findAllMembers(pageable);
        return page.map(this::mapToUserInfo);
    }

    public AuthDto.UserInfo getMemberById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member tidak ditemukan"));
        return mapToUserInfo(user);
    }

    @Transactional
    public AuthDto.UserInfo updateStatusMember(Long id, User.StatusAnggota status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member tidak ditemukan"));
        user.setStatus(status);
        return mapToUserInfo(userRepository.save(user));
    }

    @Transactional
    public AuthDto.UserInfo updateProfileMember(Long id, String namaLengkap, String noTelepon, String alamat) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member tidak ditemukan"));
        if (namaLengkap != null) user.setNamaLengkap(namaLengkap);
        if (noTelepon != null) user.setNoTelepon(noTelepon);
        if (alamat != null) user.setAlamat(alamat);
        return mapToUserInfo(userRepository.save(user));
    }

    /**
     * Admin membuat user baru dengan role tertentu (ADMIN atau MEMBER).
     */
    @Transactional
    public AuthDto.UserInfo adminCreateUser(AuthDto.AdminCreateUserRequest request) {
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
                .role(request.getRole())
                .status(User.StatusAnggota.AKTIF)
                .totalSimpanan(BigDecimal.ZERO)
                .telegramChatId(request.getTelegramChatId())
                .build();

        user = userRepository.save(user);
        log.info("Admin membuat user baru: {} - {} (role: {})",
                nomorAnggota, request.getNamaLengkap(), request.getRole());

        // Notif selamat datang ke user baru via Telegram
        kirimNotifSelamatDatang(request.getTelegramChatId(),
                request.getNamaLengkap(), nomorAnggota,
                request.getEmail(), request.getPassword());

        return mapToUserInfo(user);
    }

    /**
     * Update Chat ID Telegram user (admin atau member sendiri)
     */
    @Transactional
    public AuthDto.UserInfo updateTelegramChatId(Long id, String telegramChatId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        user.setTelegramChatId(telegramChatId);
        return mapToUserInfo(userRepository.save(user));
    }

    /**
     * Admin mengubah role user yang sudah ada (promote ke ADMIN atau demote ke MEMBER).
     */
    @Transactional
    public AuthDto.UserInfo updateRole(Long id, User.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        User.Role roleLama = user.getRole();
        user.setRole(role);
        userRepository.save(user);
        log.info("Role user {} diubah dari {} ke {}", user.getEmail(), roleLama, role);
        return mapToUserInfo(user);
    }

    private void kirimNotifSelamatDatang(String chatId, String nama,
                                         String nomorAnggota, String email, String password) {
        if (chatId == null || chatId.isBlank()) return;
        // Gunakan String.format agar tidak ada escape character yang bermasalah
        String baris1 = "Selamat Datang di Koperasi Leyangan!";
        String baris2 = "Halo " + nama + ", akun Anda telah berhasil didaftarkan.";
        String baris3 = "--- Detail Akun ---";
        String baris4 = "No. Anggota : " + nomorAnggota;
        String baris5 = "Email       : " + email;
        String baris6 = "Password    : " + password;
        String baris7 = "Silakan login di aplikasi Koperasi Leyangan.";

        String pesan = baris1 + "\n\n" + baris2 + "\n\n" + baris3 + "\n" + baris4 + "\n" + baris5 + "\n" + baris6 + "\n\n" + baris7;
        telegramService.send(chatId, pesan);
    }

    private String escMd(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }

    private String generateNomorAnggota() {
        long count = userRepository.count() + 1;
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
                .telegramChatId(user.getTelegramChatId())
                .build();
    }
}