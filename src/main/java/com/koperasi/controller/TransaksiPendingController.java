package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.dto.TransaksiPendingDto;
import com.koperasi.entity.User;
import com.koperasi.service.TransaksiPendingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pengajuan")
@RequiredArgsConstructor
public class TransaksiPendingController {

    private final TransaksiPendingService service;

    @PostMapping("/setor")
    public ResponseEntity<ApiResponse<TransaksiPendingDto.TransaksiPendingResponse>> ajukanSetor(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransaksiPendingDto.AjukanSetorRequest request) {
        var result = service.ajukanSetor(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan setoran berhasil dikirim, menunggu persetujuan admin", result));
    }

    @PostMapping("/tarik")
    public ResponseEntity<ApiResponse<TransaksiPendingDto.TransaksiPendingResponse>> ajukanTarik(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransaksiPendingDto.AjukanTarikRequest request) {
        var result = service.ajukanTarik(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan penarikan berhasil dikirim, menunggu persetujuan admin", result));
    }

    @PostMapping("/bayar-angsuran")
    public ResponseEntity<ApiResponse<TransaksiPendingDto.TransaksiPendingResponse>> ajukanBayarAngsuran(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransaksiPendingDto.AjukanBayarAngsuranRequest request) {
        var result = service.ajukanBayarAngsuran(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan pembayaran angsuran berhasil dikirim, menunggu persetujuan admin", result));
    }

    @GetMapping("/riwayat")
    public ResponseEntity<ApiResponse<Page<TransaksiPendingDto.TransaksiPendingResponse>>> getRiwayat(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 15) Pageable pageable) {
        var result = service.getRiwayatPengajuan(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}