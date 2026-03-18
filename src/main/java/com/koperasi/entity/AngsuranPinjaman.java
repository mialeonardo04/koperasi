package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "angsuran_pinjaman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AngsuranPinjaman {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinjaman_id", nullable = false)
    private Pinjaman pinjaman;

    @Column(nullable = false)
    private Integer periodeKe;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jumlahAngsuran;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal pokok;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal bunga;

    @Column
    private LocalDate tanggalJatuhTempo;

    @Column
    private LocalDate tanggalBayar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAngsuran status = StatusAngsuran.BELUM_BAYAR;

    private String noReferensiBayar;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum StatusAngsuran {
        BELUM_BAYAR, SUDAH_BAYAR, TERLAMBAT
    }
}