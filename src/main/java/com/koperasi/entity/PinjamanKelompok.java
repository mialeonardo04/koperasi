package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pinjaman_kelompok")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PinjamanKelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String noPinjaman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kelompok_id", nullable = false)
    private Kelompok kelompok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pengaju_id", nullable = false)
    private User pengaju;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jumlahPinjaman;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bungaPerBulan = new BigDecimal("1.5");

    @Column(nullable = false)
    private Integer tenorBulan;

    @Column(precision = 15, scale = 2)
    private BigDecimal angsuranPerBulan;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSudahDibayar = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal sisaPinjaman;

    @Column(columnDefinition = "TEXT")
    private String tujuanPinjaman;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPinjaman status = StatusPinjaman.PENDING;

    private LocalDate tanggalPengajuan;
    private LocalDate tanggalDisetujui;
    private LocalDate tanggalJatuhTempo;

    @Column(columnDefinition = "TEXT")
    private String keteranganAdmin;

    @OneToMany(mappedBy = "pinjamanKelompok", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AngsuranKelompok> angsuranList = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (tanggalPengajuan == null) tanggalPengajuan = LocalDate.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum StatusPinjaman {
        PENDING, DISETUJUI, DITOLAK, LUNAS, MACET
    }
}