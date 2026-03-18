package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "angsuran_kelompok")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AngsuranKelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinjaman_kelompok_id", nullable = false)
    private PinjamanKelompok pinjamanKelompok;

    @Column(nullable = false)
    private Integer periodeKe;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jumlahAngsuran;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal pokok;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal bunga;

    @Column(nullable = false)
    private LocalDate tanggalJatuhTempo;

    private LocalDate tanggalBayar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAngsuran status = StatusAngsuran.BELUM_BAYAR;

    // Siapa yang membayar angsuran ini (bisa anggota lain dalam kelompok)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dibayar_oleh_user_id")
    private User dibayarOleh;

    @Enumerated(EnumType.STRING)
    private MetodeBayar metodeBayar;

    private String noReferensiBayar;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum StatusAngsuran {
        BELUM_BAYAR, SUDAH_BAYAR, TERLAMBAT
    }

    public enum MetodeBayar {
        SIMPANAN,   // debit dari saldo simpanan
        TRANSFER    // bukti transfer manual
    }
}