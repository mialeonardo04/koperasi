package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pinjaman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pinjaman {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String noPinjaman;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jumlahPinjaman;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal bungaPerBulan;

    @Column(nullable = false)
    private Integer tenorBulan;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal angsuranPerBulan;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSudahDibayar = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal sisaPinjaman;

    @Column
    private LocalDate tanggalPengajuan;

    @Column
    private LocalDate tanggalDisetujui;

    @Column
    private LocalDate tanggalJatuhTempo;

    @Column(columnDefinition = "TEXT")
    private String tujuanPinjaman;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPinjaman status = StatusPinjaman.PENDING;

    @Column(columnDefinition = "TEXT")
    private String keteranganAdmin;

    @OneToMany(mappedBy = "pinjaman", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AngsuranPinjaman> angsuranList = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tanggalPengajuan == null) tanggalPengajuan = LocalDate.now();
        if (noPinjaman == null) noPinjaman = "PIN-" + System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum StatusPinjaman {
        PENDING, DISETUJUI, DITOLAK, LUNAS, MACET
    }
}