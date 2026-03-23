package com.koperasi.controller;

import com.koperasi.dto.*;
import com.koperasi.dto.KelompokDto;
import com.koperasi.entity.User;
import com.koperasi.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserManagementService userManagementService;
    private final SimpananService simpananService;
    private final PinjamanService pinjamanService;
    private final ReportService reportService;
    private final TransaksiPendingService transaksiPendingService;
    private final KelompokService kelompokService;

    // ========== MEMBER MANAGEMENT ==========

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<Page<AuthDto.UserInfo>>> getAllMembers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getAllMembers(search, pageable)));
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userManagementService.getMemberById(id)));
    }

    @PutMapping("/members/{id}/status")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateStatusMember(
            @PathVariable Long id, @RequestParam User.StatusAnggota status) {
        return ResponseEntity.ok(ApiResponse.ok("Status anggota diperbarui",
                userManagementService.updateStatusMember(id, status)));
    }

    @PutMapping("/members/{id}/profile")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateProfileMember(
            @PathVariable Long id,
            @RequestParam(required = false) String namaLengkap,
            @RequestParam(required = false) String noTelepon,
            @RequestParam(required = false) String alamat) {
        return ResponseEntity.ok(ApiResponse.ok("Profil anggota diperbarui",
                userManagementService.updateProfileMember(id, namaLengkap, noTelepon, alamat)));
    }

    /** Admin membuat user baru — bisa pilih role ADMIN atau MEMBER */
    @PostMapping("/members/create")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> adminCreateUser(
            @Valid @RequestBody AuthDto.AdminCreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User berhasil dibuat",
                userManagementService.adminCreateUser(request)));
    }

    /** Admin mengubah role user (promote/demote) */
    @PutMapping("/members/{id}/role")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody AuthDto.UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Role berhasil diperbarui",
                userManagementService.updateRole(id, request.getRole())));
    }

    /** Update Chat ID Telegram anggota untuk notifikasi */
    @PutMapping("/members/{id}/telegram")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateTelegramChatId(
            @PathVariable Long id,
            @RequestParam(required = false) String chatId) {
        return ResponseEntity.ok(ApiResponse.ok("Chat ID Telegram diperbarui",
                userManagementService.updateTelegramChatId(id, chatId)));
    }

    // ========== SIMPANAN MANAGEMENT ==========

    @PostMapping("/simpanan/setor/{userId}")
    public ResponseEntity<ApiResponse<SimpananDto.SimpananResponse>> setorUntukMember(
            @PathVariable Long userId, @Valid @RequestBody SimpananDto.SetorRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Setoran berhasil dicatat", simpananService.setor(userId, request)));
    }

    @PostMapping("/simpanan/tarik/{userId}")
    public ResponseEntity<ApiResponse<SimpananDto.SimpananResponse>> tarikUntukMember(
            @PathVariable Long userId, @Valid @RequestBody SimpananDto.TarikRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Penarikan berhasil dicatat", simpananService.tarik(userId, request)));
    }

    @GetMapping("/simpanan")
    public ResponseEntity<ApiResponse<Page<SimpananDto.SimpananResponse>>> getAllSimpanan(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(simpananService.getAllSimpanan(userId, null, null, pageable)));
    }

    @GetMapping("/simpanan/saldo/{userId}")
    public ResponseEntity<ApiResponse<SimpananDto.SaldoResponse>> getSaldoMember(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(simpananService.getSaldo(userId)));
    }

    // ========== PINJAMAN MANAGEMENT ==========

    @GetMapping("/pinjaman/pending-count")
    public ResponseEntity<ApiResponse<Long>> getPinjamanPendingCount() {
        return ResponseEntity.ok(ApiResponse.ok(pinjamanService.countPending()));
    }

    @GetMapping("/pinjaman")
    public ResponseEntity<ApiResponse<Page<PinjamanDto.PinjamanResponse>>> getAllPinjaman(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(pinjamanService.getAllPinjaman(userId, status, pageable)));
    }

    @PutMapping("/pinjaman/{id}/proses")
    public ResponseEntity<ApiResponse<PinjamanDto.PinjamanResponse>> prosesPinjaman(
            @PathVariable Long id, @Valid @RequestBody PinjamanDto.ApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Pinjaman berhasil diproses",
                pinjamanService.prosesPersetujuan(id, request)));
    }

    @GetMapping("/pinjaman/{id}/jadwal-angsuran")
    public ResponseEntity<ApiResponse<List<PinjamanDto.AngsuranResponse>>> getJadwalAngsuranAdmin(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pinjamanService.getJadwalAngsuran(id, null, true)));
    }

    // ========== APPROVAL TRANSAKSI PENDING ==========

    @GetMapping("/pengajuan/summary")
    public ResponseEntity<ApiResponse<TransaksiPendingDto.PendingSummary>> getPendingSummary() {
        return ResponseEntity.ok(ApiResponse.ok(transaksiPendingService.getPendingSummary()));
    }

    @GetMapping("/pengajuan/pending")
    public ResponseEntity<ApiResponse<List<TransaksiPendingDto.TransaksiPendingResponse>>> getAllPending() {
        return ResponseEntity.ok(ApiResponse.ok(transaksiPendingService.getAllPending()));
    }

    @GetMapping("/pengajuan")
    public ResponseEntity<ApiResponse<Page<TransaksiPendingDto.TransaksiPendingResponse>>> getAllPengajuan(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(transaksiPendingService.getAllTransaksi(status, pageable)));
    }

    @PutMapping("/pengajuan/{id}/proses")
    public ResponseEntity<ApiResponse<TransaksiPendingDto.TransaksiPendingResponse>> prosesPengajuan(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody TransaksiPendingDto.ApprovalRequest request) {
        var result = transaksiPendingService.prosesApproval(id, admin.getId(), request);
        String msg = request.getDisetujui() ? "Pengajuan berhasil disetujui" : "Pengajuan berhasil ditolak";
        return ResponseEntity.ok(ApiResponse.ok(msg, result));
    }

    // ========== REPORT ==========

    @GetMapping("/report/dashboard")
    public ResponseEntity<ApiResponse<ReportDto.DashboardAdmin>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDashboardAdmin()));
    }

    @GetMapping("/report/rekap-simpanan")
    public ResponseEntity<ApiResponse<List<ReportDto.RekapSimpanan>>> getRekapSimpanan(
            @RequestParam(defaultValue = "0") int bulan,
            @RequestParam(defaultValue = "0") int tahun) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getRekapSimpananAllMember(bulan, tahun)));
    }

    @GetMapping("/report/rekap-pinjaman")
    public ResponseEntity<ApiResponse<List<ReportDto.RekapPinjaman>>> getRekapPinjaman() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getRekapPinjamanAllMember()));
    }

    // ========== KELOMPOK MANAGEMENT ==========

    @GetMapping("/kelompok")
    public ResponseEntity<ApiResponse<Page<KelompokDto.KelompokResponse>>> getAllKelompok(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(kelompokService.getAllKelompok(pageable)));
    }

    @GetMapping("/kelompok/{id}")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> getKelompokDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(kelompokService.getKelompokDetail(id)));
    }

    @PutMapping("/kelompok/pinjaman/{id}/proses")
    public ResponseEntity<ApiResponse<KelompokDto.PinjamanKelompokResponse>> prosesPinjamanKelompok(
            @PathVariable Long id,
            @RequestParam boolean disetujui,
            @RequestParam(required = false) String keteranganAdmin) {
        return ResponseEntity.ok(ApiResponse.ok(
                disetujui ? "Pinjaman kelompok disetujui" : "Pinjaman kelompok ditolak",
                kelompokService.prosesPinjaman(id, disetujui, keteranganAdmin)));
    }

    @PostMapping("/kelompok/{id}/teguran")
    public ResponseEntity<ApiResponse<KelompokDto.TeguranResponse>> kirimTeguran(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody KelompokDto.KirimTeguranRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Teguran berhasil dikirim",
                kelompokService.kirimTeguran(admin.getId(), id, req)));
    }

    // ── Admin Kelola Kelompok ─────────────────────────────────────────────

    /** Admin buat kelompok atas nama member (sebagai leader) */
    @PostMapping("/kelompok")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> adminBuatKelompok(
            @Valid @RequestBody KelompokDto.AdminBuatKelompokRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Kelompok berhasil dibuat",
                kelompokService.buatKelompokAsUser(req.getLeaderId(), req)));
    }

    /** Admin tambah anggota ke kelompok mana saja */
    @PostMapping("/kelompok/{id}/anggota")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> adminTambahAnggota(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody KelompokDto.TambahAnggotaRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Anggota berhasil ditambahkan",
                kelompokService.tambahAnggota(id, admin.getId(), req)));
    }

    /** Admin ganti leader kelompok */
    @PutMapping("/kelompok/{id}/leader")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> adminGantiLeader(
            @PathVariable Long id,
            @RequestBody KelompokDto.GantiLeaderRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Leader berhasil diganti",
                kelompokService.gantiLeader(id, req.getNewLeaderId())));
    }

    /** Admin bubarkan kelompok */
    @DeleteMapping("/kelompok/{id}")
    public ResponseEntity<ApiResponse<KelompokDto.KelompokResponse>> adminBubarkanKelompok(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.ok("Kelompok berhasil dibubarkan",
                kelompokService.bubarkanKelompok(id, admin.getId())));
    }
}