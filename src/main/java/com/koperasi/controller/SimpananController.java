package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.dto.SimpananDto;
import com.koperasi.entity.User;
import com.koperasi.service.SimpananService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/simpanan")
@RequiredArgsConstructor
public class SimpananController {

    private final SimpananService simpananService;

    @PostMapping("/setor")
    public ResponseEntity<ApiResponse<SimpananDto.SimpananResponse>> setor(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SimpananDto.SetorRequest request) {
        SimpananDto.SimpananResponse response = simpananService.setor(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Setoran berhasil", response));
    }

    @PostMapping("/tarik")
    public ResponseEntity<ApiResponse<SimpananDto.SimpananResponse>> tarik(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SimpananDto.TarikRequest request) {
        SimpananDto.SimpananResponse response = simpananService.tarik(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Penarikan berhasil", response));
    }

    @GetMapping("/riwayat")
    public ResponseEntity<ApiResponse<Page<SimpananDto.SimpananResponse>>> getRiwayat(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "tanggalTransaksi") Pageable pageable) {
        Page<SimpananDto.SimpananResponse> riwayat = simpananService.getRiwayat(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(riwayat));
    }

    @GetMapping("/saldo")
    public ResponseEntity<ApiResponse<SimpananDto.SaldoResponse>> getSaldo(
            @AuthenticationPrincipal User user) {
        SimpananDto.SaldoResponse saldo = simpananService.getSaldo(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(saldo));
    }
}