package com.koperasi.controller;

import com.koperasi.entity.User;
import com.koperasi.repository.UserRepository;
import com.koperasi.security.JwtUtil;
import com.koperasi.service.SlipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/slip")
@RequiredArgsConstructor
public class SlipController {

    private final SlipService    slipService;
    private final JwtUtil        jwtUtil;
    private final UserRepository userRepository;

    private Long resolveUserId(String authHeader) {
        String email = jwtUtil.extractEmailFromHeader(authHeader);
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    /**
     * Download slip setoran/penarikan simpanan
     * GET /api/slip/setoran/{transaksiId}
     */
    @GetMapping("/setoran/{transaksiId}")
    public ResponseEntity<byte[]> slipSetoran(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long transaksiId) {
        resolveUserId(authHeader); // validasi token
        byte[] pdf = slipService.generateSlipSetoran(transaksiId);
        return pdfResponse(pdf, "slip-setoran-" + transaksiId + ".pdf");
    }

    /**
     * Download slip bayar angsuran kelompok
     * GET /api/slip/angsuran/{angsuranId}
     */
    @GetMapping("/angsuran/{angsuranId}")
    public ResponseEntity<byte[]> slipAngsuran(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long angsuranId) {
        Long userId = resolveUserId(authHeader);
        byte[] pdf = slipService.generateSlipAngsuran(angsuranId, userId);
        return pdfResponse(pdf, "slip-angsuran-" + angsuranId + ".pdf");
    }

    /**
     * Download kartu simpanan member
     * GET /api/slip/kartu-simpanan
     */
    @GetMapping("/kartu-simpanan")
    public ResponseEntity<byte[]> kartuSimpanan(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = resolveUserId(authHeader);
        byte[] pdf = slipService.generateKartuSimpanan(userId);
        return pdfResponse(pdf, "kartu-simpanan.pdf");
    }
}