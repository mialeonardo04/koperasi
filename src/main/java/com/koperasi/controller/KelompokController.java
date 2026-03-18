package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.dto.AuthDto;
import com.koperasi.service.UserManagementService;
import com.koperasi.dto.KelompokDto;
import com.koperasi.entity.User;
import com.koperasi.service.KelompokService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kelompok")
@RequiredArgsConstructor
public class KelompokController {

    private final KelompokService kelompokService;
    private final UserManagementService userManagementService;

    // ── Kelompok ──────────────────────────────────────────────

    /** Buat kelompok baru — otomatis jadi leader */
    @PostMapping
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> buatKelompok(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody KelompokDto.BuatKelompokRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Kelompok berhasil dibuat",
                kelompokService.buatKelompok(user.getId(), req)));
    }

    /** Lihat kelompok saya saat ini */
    @GetMapping("/saya")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> getKelompokSaya(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(kelompokService.getKelompokSaya(user.getId())));
    }

    /** Detail kelompok by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> getDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kelompokService.getKelompokDetail(id)));
    }

    /** Tambah anggota ke kelompok (hanya leader) */
    @PostMapping("/{id}/anggota")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> tambahAnggota(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody KelompokDto.TambahAnggotaRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Anggota berhasil ditambahkan",
                kelompokService.tambahAnggota(id, user.getId(), req)));
    }

    /** Bubarkan kelompok (hanya leader, jika tidak ada pinjaman aktif) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> bubarkanKelompok(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Kelompok berhasil dibubarkan",
                kelompokService.bubarkanKelompok(id, user.getId())));
    }

    /** Keluar dari kelompok */
    @DeleteMapping("/{id}/anggota")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> keluarKelompok(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Berhasil keluar dari kelompok",
                kelompokService.keluarKelompok(id, user.getId())));
    }

    /**
     * Ambil semua member yang belum terikat kelompok aktif — untuk tabel pilih anggota
     */
    @GetMapping("/member-bebas")
    public ResponseEntity<ApiResponse<?>> getMemberBebas() {
        return ResponseEntity.ok(ApiResponse.ok(
                kelompokService.getMemberBebasKelompok()));
    }

    /**
     * Cari member lain untuk ditambahkan ke kelompok.
     * Accessible oleh semua member yang sudah login.
     * GET /kelompok/cari-member?search=budi
     */
    @GetMapping("/cari-member")
    public ResponseEntity<ApiResponse<?>> cariMember(
            @RequestParam(required = false) String search,
            @org.springframework.data.web.PageableDefault(size = 8) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                userManagementService.getAllMembers(search, pageable)));
    }

    // ── Pinjaman Kelompok ─────────────────────────────────────

    /** Get detail pinjaman kelompok by ID (dengan jadwal angsuran lengkap) */
    @GetMapping("/pinjaman/{pinjamanId}")
    public ResponseEntity<ApiResponse<KelompokDto.PinjamanKelompokResponse>> getPinjamanDetail(
            @PathVariable Long pinjamanId) {
        return ResponseEntity.ok(ApiResponse.ok(
                kelompokService.getPinjamanDetail(pinjamanId)));
    }

    /** Ajukan pinjaman kelompok */
    @PostMapping("/{id}/pinjaman")
    public ResponseEntity<ApiResponse<KelompokDto.PinjamanKelompokResponse>> ajukanPinjaman(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody KelompokDto.AjukanPinjamanRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan pinjaman kelompok berhasil dikirim",
                kelompokService.ajukanPinjaman(user.getId(), id, req)));
    }

    // ── Bayar Angsuran ────────────────────────────────────────

    /** Ajukan bayar angsuran kelompok (bisa anggota mana saja dalam kelompok) */
    @PostMapping("/bayar-angsuran")
    public ResponseEntity<ApiResponse<String>> ajukanBayarAngsuran(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody KelompokDto.BayarAngsuranRequest req) {
        kelompokService.ajukanBayarAngsuran(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Pengajuan pembayaran angsuran berhasil dikirim, menunggu persetujuan admin"));
    }

    // ── Pencairan Dana ───────────────────────────────────────

    /** Anggota request cairkan dana dari pool pinjaman kelompok */
    @PostMapping("/pinjaman/{pinjamanId}/cairkan")
    public ResponseEntity<ApiResponse<KelompokDto.PencairanResponse>> requestPencairan(
            @PathVariable Long pinjamanId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody KelompokDto.RequestPencairanRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Request pencairan berhasil dikirim ke leader",
                kelompokService.requestPencairan(user.getId(), pinjamanId, req)));
    }

    /** Leader setujui/tolak request pencairan */
    @PutMapping("/pencairan/{pencairanId}/proses")
    public ResponseEntity<ApiResponse<KelompokDto.PencairanResponse>> prosesPencairan(
            @PathVariable Long pencairanId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody KelompokDto.ProsesPencairanRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                req.getDisetujui() ? "Pencairan disetujui" : "Pencairan ditolak",
                kelompokService.prosesPencairan(user.getId(), pencairanId, req)));
    }

    /** Leader lihat request pencairan yang pending */
    @GetMapping("/{id}/pencairan/pending")
    public ResponseEntity<ApiResponse<List<KelompokDto.PencairanResponse>>> getPencairanPending(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                kelompokService.getPencairanPending(id, user.getId())));
    }

    /** Lihat riwayat pencairan pinjaman kelompok */
    @GetMapping("/pinjaman/{pinjamanId}/pencairan")
    public ResponseEntity<ApiResponse<List<KelompokDto.PencairanResponse>>> getRiwayatPencairan(
            @PathVariable Long pinjamanId) {
        return ResponseEntity.ok(ApiResponse.ok(
                kelompokService.getRiwayatPencairan(pinjamanId)));
    }

    // ── Teguran ───────────────────────────────────────────────

    /** Lihat teguran yang diterima kelompok */
    @GetMapping("/{id}/teguran")
    public ResponseEntity<ApiResponse<List<KelompokDto.TeguranResponse>>> getTeguran(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kelompokService.getTeguranKelompok(id)));
    }
}