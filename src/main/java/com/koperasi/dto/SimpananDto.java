package com.koperasi.dto;

import com.koperasi.entity.Simpanan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SimpananDto {

    @Data
    public static class SetorRequest {
        @NotNull(message = "Jenis simpanan wajib diisi")
        private Simpanan.JenisSimpanan jenis;

        @NotNull(message = "Jumlah wajib diisi")
        @DecimalMin(value = "1000", message = "Jumlah minimal Rp 1.000")
        private BigDecimal jumlah;

        private String keterangan;
    }

    @Data
    public static class TarikRequest {
        @NotNull(message = "Jenis simpanan wajib diisi")
        private Simpanan.JenisSimpanan jenis;

        @NotNull(message = "Jumlah wajib diisi")
        @DecimalMin(value = "1000", message = "Jumlah minimal Rp 1.000")
        private BigDecimal jumlah;

        private String keterangan;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpananResponse {
        private Long id;
        private Long userId;
        private String namaAnggota;
        private String nomorAnggota;
        private String jenis;
        private BigDecimal jumlah;
        private String tipe;
        private String keterangan;
        private LocalDateTime tanggalTransaksi;
        private String noReferensi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaldoResponse {
        private BigDecimal simpananPokok;
        private BigDecimal simpananWajib;
        private BigDecimal simpananSukarela;
        private BigDecimal totalSimpanan;
    }
}