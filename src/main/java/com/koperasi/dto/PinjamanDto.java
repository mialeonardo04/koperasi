package com.koperasi.dto;

import com.koperasi.entity.AngsuranPinjaman;
import com.koperasi.entity.Pinjaman;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PinjamanDto {

    @Data
    public static class PengajuanRequest {
        @NotNull(message = "Jumlah pinjaman wajib diisi")
        @DecimalMin(value = "500000", message = "Pinjaman minimal Rp 500.000")
        private BigDecimal jumlahPinjaman;

        @NotNull(message = "Tenor wajib diisi")
        @Min(value = 1, message = "Tenor minimal 1 bulan")
        @Max(value = 60, message = "Tenor maksimal 60 bulan")
        private Integer tenorBulan;

        private String tujuanPinjaman;
    }

    @Data
    public static class ApprovalRequest {
        @NotNull
        private Boolean disetujui;
        private String keteranganAdmin;
        private BigDecimal bungaPerBulan;
    }

    @Data
    public static class BayarAngsuranRequest {
        @NotNull
        private Long angsuranId;
        private String keterangan;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PinjamanResponse {
        private Long id;
        private String noPinjaman;
        private Long userId;
        private String namaAnggota;
        private String nomorAnggota;
        private BigDecimal jumlahPinjaman;
        private BigDecimal bungaPerBulan;
        private Integer tenorBulan;
        private BigDecimal angsuranPerBulan;
        private BigDecimal totalSudahDibayar;
        private BigDecimal sisaPinjaman;
        private LocalDate tanggalPengajuan;
        private LocalDate tanggalDisetujui;
        private LocalDate tanggalJatuhTempo;
        private String tujuanPinjaman;
        private String status;
        private String keteranganAdmin;
        private List<AngsuranResponse> angsuranList;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngsuranResponse {
        private Long id;
        private Integer periodeKe;
        private BigDecimal jumlahAngsuran;
        private BigDecimal pokok;
        private BigDecimal bunga;
        private LocalDate tanggalJatuhTempo;
        private LocalDate tanggalBayar;
        private String status;
        private String noReferensiBayar;
    }
}