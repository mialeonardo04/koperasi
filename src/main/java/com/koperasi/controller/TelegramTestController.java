package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/telegram")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TelegramTestController {

    private final TelegramService telegramService;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    /**
     * Test kirim pesan ke Chat ID tertentu
     * GET /api/admin/telegram/test?chatId=436703107
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test(
            @RequestParam(required = false) String chatId) {
        String target = (chatId != null && !chatId.isBlank()) ? chatId : adminChatId;
        telegramService.send(target,
                "✅ *Test Notifikasi Koperasi Leyangan*\n\n" +
                        "Halo\\! Bot Telegram Koperasi Leyangan berhasil terhubung\\.\n" +
                        "Anda akan menerima notifikasi di sini\\.");
        return ResponseEntity.ok(ApiResponse.ok("Pesan test dikirim ke chat_id: " + target));
    }
}