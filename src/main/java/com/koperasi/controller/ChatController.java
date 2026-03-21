package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import com.koperasi.security.JwtUtil;
import com.koperasi.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService    chatService;
    private final JwtUtil        jwtUtil;
    private final UserRepository userRepository;

    private Long resolveUserId(String authHeader) {
        String email = jwtUtil.extractEmailFromHeader(authHeader);
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<?>> chat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        Long userId = resolveUserId(authHeader);
        String pesan = body.get("pesan");

        if (pesan == null || pesan.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Pesan tidak boleh kosong"));
        }

        String balasan = chatService.chat(userId, pesan);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("balasan", balasan)));
    }

    @DeleteMapping("/reset")
    public ResponseEntity<ApiResponse<?>> resetHistory(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = resolveUserId(authHeader);
        chatService.resetHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok("Riwayat chat direset"));
    }
}