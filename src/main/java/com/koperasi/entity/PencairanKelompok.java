package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pencairan_kelompok")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PencairanKelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinjaman_kelompok_id", nullable = false)
    private PinjamanKelompok pinjamanKelompok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jumlah;

    /** Jatah normal = total pinjaman / jumlah anggota saat pengajuan */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal jatahRata;

    /** True jika jumlah > jatahRata */
    @Column(nullable = false)
    @Builder.Default
    private Boolean melebihiJatah = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPencairan status = StatusPencairan.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disetujui_oleh")
    private User disetujuiOleh;

    @Column(columnDefinition = "TEXT")
    private String catatan;

    private LocalDateTime tanggalApproval;

    /** Jika melebihi jatah, wajib bayar minimal 1 angsuran */
    @Column(nullable = false)
    @Builder.Default
    private Boolean wajibBayarAngsuran = false;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum StatusPencairan {
        PENDING,    // menunggu approval leader
        DISETUJUI,
        DITOLAK
    }
}