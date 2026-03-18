package com.koperasi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class ReportDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardAdmin {
        private Long totalAnggota;
        private Long anggotaAktif;
        private BigDecimal totalSimpanan;
        private BigDecimal totalPinjamanAktif;
        private BigDecimal totalSisaPinjaman;
        private Long pinjamanPending;
        private Long pinjamanAktif;
        private Long angsuranJatuhTempo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMember {
        private String nomorAnggota;
        private String namaLengkap;
        private BigDecimal simpananPokok;
        private BigDecimal simpananWajib;
        private BigDecimal simpananSukarela;
        private BigDecimal totalSimpanan;
        private Long pinjamanAktif;
        private BigDecimal totalPinjaman;
        private BigDecimal sisaPinjaman;
        private Long angsuranTerlambat;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RekapSimpanan {
        private String nomorAnggota;
        private String namaAnggota;
        private BigDecimal simpananPokok;
        private BigDecimal simpananWajib;
        private BigDecimal simpananSukarela;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RekapPinjaman {
        private String nomorAnggota;
        private String namaAnggota;
        private String noPinjaman;
        private BigDecimal jumlahPinjaman;
        private BigDecimal sudahDibayar;
        private BigDecimal sisaPinjaman;
        private String status;
    }
}