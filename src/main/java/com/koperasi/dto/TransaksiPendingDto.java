package com.koperasi.dto;

import com.koperasi.entity.Simpanan;
import com.koperasi.entity.TransaksiPending;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransaksiPendingDto {

    @Data
    public static class AjukanSetorRequest {
        @NotNull(message = "Jenis simpanan wajib diisi")
        private Simpanan.JenisSimpanan jenisSimpanan;

        @NotNull(message = "Jumlah wajib diisi")
        @DecimalMin(value = "1000", message = "Jumlah minimal Rp 1.000")
        private BigDecimal jumlah;

        private String keterangan;
        private String buktiBayar; // path dari upload endpoint
    }

    @Data
    public static class AjukanTarikRequest {
        @NotNull(message = "Jenis simpanan wajib diisi")
        private Simpanan.JenisSimpanan jenisSimpanan;

        @NotNull(message = "Jumlah wajib diisi")
        @DecimalMin(value = "1000", message = "Jumlah minimal Rp 1.000")
        private BigDecimal jumlah;

        private String keterangan;
        private String buktiBayar;
    }

    @Data
    public static class AjukanBayarAngsuranRequest {
        @NotNull(message = "ID angsuran wajib diisi")
        private Long angsuranId;

        private String keterangan;
        private String buktiBayar; // path dari upload endpoint
    }

    @Data
    public static class ApprovalRequest {
        @NotNull
        private Boolean disetujui;
        private String catatanAdmin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransaksiPendingResponse {
        private Long id;
        private Long userId;
        private String namaAnggota;
        private String nomorAnggota;
        private String jenisTransaksi;
        private String jenisSimpanan;
        private BigDecimal jumlah;
        private String keterangan;
        private Long angsuranId;
        private Integer periodeAngsuran;
        private String noPinjaman;
        private String status;
        private String catatanAdmin;
        private String buktiBayar;
        private String approvedByName;
        private LocalDateTime tanggalApproval;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingSummary {
        private Long totalPending;
    }
}