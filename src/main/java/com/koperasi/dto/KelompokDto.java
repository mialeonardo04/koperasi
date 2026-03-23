package com.koperasi.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class KelompokDto {

    // ── Request ──────────────────────────────────────────────

    @Data
    public static class BuatKelompokRequest {
        @NotBlank(message = "Nama kelompok wajib diisi")
        @Size(min = 3, max = 100)
        private String namaKelompok;
        private String deskripsi;
    }

    @Data
    public static class TambahAnggotaRequest {
        @NotNull(message = "ID anggota wajib diisi")
        private Long userId;
    }

    @Data
    public static class AjukanPinjamanRequest {
        @NotNull
        @DecimalMin(value = "100000", message = "Minimal pinjaman Rp 100.000")
        @DecimalMax(value = "3000000", message = "Maksimal pinjaman Rp 3.000.000")
        private BigDecimal jumlahPinjaman;

        @NotNull
        @Min(value = 1, message = "Tenor minimal 1 bulan")
        @Max(value = 10, message = "Tenor maksimal 10 bulan")
        private Integer tenorBulan;

        private String tujuanPinjaman;
    }

    @Data
    public static class BayarAngsuranRequest {
        @NotNull
        private Long angsuranId;
        private String metodeBayar;   // SIMPANAN atau TRANSFER
        private String buktiBayar;    // path foto jika TRANSFER
        private String jenisSimpanan; // SUKARELA / WAJIB / POKOK jika SIMPANAN
        private String keterangan;
    }

    @Data
    public static class RequestPencairanRequest {
        @NotNull(message = "Jumlah wajib diisi")
        @DecimalMin(value = "10000", message = "Minimal pencairan Rp 10.000")
        private BigDecimal jumlah;
        private String catatan;
    }

    @Data
    public static class ProsesPencairanRequest {
        @NotNull
        private Boolean disetujui;
        private String catatan;
    }

    @Data
    public static class KirimTeguranRequest {
        @NotBlank(message = "Pesan teguran wajib diisi")
        private String pesan;
    }

    // ── Response ─────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KelompokResponse {
        private Long id;
        private String kodeKelompok;
        private String namaKelompok;
        private String deskripsi;
        private Long leaderId;
        private String namaLeader;
        private String status;
        private int jumlahAnggota;
        private List<AnggotaInfo> anggotaList;
        private PinjamanKelompokResponse pinjamanAktif;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AnggotaInfo {
        private Long id;
        private String namaLengkap;
        private String nomorAnggota;
        private boolean isLeader;
        private LocalDateTime joinedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PinjamanKelompokResponse {
        private Long id;
        private String noPinjaman;
        private Long kelompokId;
        private String namaKelompok;
        private Long pengajuId;
        private String namaPengaju;
        private BigDecimal jumlahPinjaman;
        private BigDecimal bungaPerBulan;
        private Integer tenorBulan;
        private BigDecimal angsuranPerBulan;
        private BigDecimal totalSudahDibayar;
        private BigDecimal sisaPinjaman;
        private String tujuanPinjaman;
        private String status;
        private LocalDate tanggalPengajuan;
        private LocalDate tanggalDisetujui;
        private LocalDate tanggalJatuhTempo;
        private String keteranganAdmin;
        private List<AngsuranKelompokResponse> jadwalAngsuran;
        private List<PencairanResponse> riwayatPencairan;
        private BigDecimal totalTercairkan;    // total yang sudah dicairkan anggota
        private BigDecimal sisaPool;           // sisa dana yang belum dicairkan
        private BigDecimal jatahRataPerAnggota;// jatah normal per anggota
        private int jumlahAnggota;
        private long pencairanPending;         // jumlah request pencairan menunggu approval leader
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AngsuranKelompokResponse {
        private Long id;
        private Integer periodeKe;
        private BigDecimal jumlahAngsuran;
        private BigDecimal pokok;
        private BigDecimal bunga;
        private LocalDate tanggalJatuhTempo;
        private LocalDate tanggalBayar;
        private String status;
        private String dibayarOleh;
        private String metodeBayar;
        private boolean terlambat;
        private boolean adaPendingBayar;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PencairanResponse {
        private Long id;
        private Long userId;
        private String namaAnggota;
        private BigDecimal jumlah;
        private BigDecimal jatahRata;
        private Boolean melebihiJatah;
        private Boolean wajibBayarAngsuran;
        private String status;
        private String namaLeaderApproval;
        private String catatan;
        private LocalDateTime tanggalApproval;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TeguranResponse {
        private Long id;
        private String namaAdmin;
        private String pesan;
        private LocalDateTime sentAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminBuatKelompokRequest {
        @NotBlank(message = "Nama kelompok wajib diisi")
        private String namaKelompok;
        @NotNull(message = "Leader wajib dipilih")
        private Long leaderId;
        private String deskripsi;

        // Agar bisa dipakai sebagai BuatKelompokRequest
        public String getNamaKelompok() { return namaKelompok; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GantiLeaderRequest {
        @NotNull(message = "Leader baru wajib dipilih")
        private Long newLeaderId;
    }
}