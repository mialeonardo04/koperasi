package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.dto.AuthDto;
import com.koperasi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login berhasil", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.UserInfo userInfo = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registrasi berhasil", userInfo));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthDto.UserInfo profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password berhasil diubah", null));
    }
}