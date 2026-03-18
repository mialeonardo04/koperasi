package com.koperasi.dto;

import com.koperasi.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email tidak boleh kosong")
        @Email(message = "Format email tidak valid")
        private String email;

        @NotBlank(message = "Password tidak boleh kosong")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UserInfo user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String nomorAnggota;
        private String namaLengkap;
        private String email;
        private String role;
        private String status;
        private BigDecimal totalSimpanan;
        private String telegramChatId;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Nama lengkap tidak boleh kosong")
        private String namaLengkap;

        @NotBlank(message = "Email tidak boleh kosong")
        @Email(message = "Format email tidak valid")
        private String email;

        @NotBlank(message = "Password tidak boleh kosong")
        @Size(min = 6, message = "Password minimal 6 karakter")
        private String password;

        private String noTelepon;
        private String alamat;
    }

    // Request tambah user oleh admin — bisa pilih role ADMIN atau MEMBER
    @Data
    public static class AdminCreateUserRequest {
        @NotBlank(message = "Nama lengkap tidak boleh kosong")
        private String namaLengkap;

        @NotBlank(message = "Email tidak boleh kosong")
        @Email(message = "Format email tidak valid")
        private String email;

        @NotBlank(message = "Password tidak boleh kosong")
        @Size(min = 6, message = "Password minimal 6 karakter")
        private String password;

        private String noTelepon;
        private String alamat;

        @NotNull(message = "Role wajib dipilih")
        private User.Role role; // ADMIN atau MEMBER
        private String telegramChatId;
    }

    // Request promote/demote role user yang sudah ada
    @Data
    public static class UpdateRoleRequest {
        @NotNull(message = "Role wajib dipilih")
        private User.Role role;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String passwordLama;

        @NotBlank
        @Size(min = 6)
        private String passwordBaru;
    }
}