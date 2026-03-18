package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.dto.PinjamanDto;
import com.koperasi.entity.User;
import com.koperasi.service.PinjamanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pinjaman")
@RequiredArgsConstructor
public class PinjamanController {

    private final PinjamanService pinjamanService;

    @PostMapping("/ajukan")
    public ResponseEntity<ApiResponse<PinjamanDto.PinjamanResponse>> ajukan(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PinjamanDto.PengajuanRequest request) {
        PinjamanDto.PinjamanResponse response = pinjamanService.ajukanPinjaman(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan pinjaman berhasil dikirim", response));
    }

    @PostMapping("/{id}/bayar-angsuran")
    public ResponseEntity<ApiResponse<PinjamanDto.PinjamanResponse>> bayarAngsuran(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody PinjamanDto.BayarAngsuranRequest request) {
        PinjamanDto.PinjamanResponse response = pinjamanService.bayarAngsuran(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Pembayaran angsuran berhasil", response));
    }

    @GetMapping("/riwayat")
    public ResponseEntity<ApiResponse<Page<PinjamanDto.PinjamanResponse>>> getRiwayat(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<PinjamanDto.PinjamanResponse> riwayat = pinjamanService.getRiwayatPinjaman(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(riwayat));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PinjamanDto.PinjamanResponse>> getDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        PinjamanDto.PinjamanResponse response = pinjamanService.getDetailPinjaman(id, user.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/jadwal-angsuran")
    public ResponseEntity<ApiResponse<List<PinjamanDto.AngsuranResponse>>> getJadwal(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        List<PinjamanDto.AngsuranResponse> jadwal = pinjamanService.getJadwalAngsuran(id, user.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(jadwal));
    }
}