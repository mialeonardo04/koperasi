package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "simpanan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simpanan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JenisSimpanan jenis;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jumlah;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TipeTransaksi tipe = TipeTransaksi.SETOR;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    @Column(nullable = false, updatable = false)
    private LocalDateTime tanggalTransaksi;

    private String noReferensi;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tanggalTransaksi == null) tanggalTransaksi = LocalDateTime.now();
        if (noReferensi == null) noReferensi = generateNoRef();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateNoRef() {
        return "SMP-" + System.currentTimeMillis();
    }

    public enum JenisSimpanan {
        POKOK, WAJIB, SUKARELA
    }

    public enum TipeTransaksi {
        SETOR, TARIK
    }
}