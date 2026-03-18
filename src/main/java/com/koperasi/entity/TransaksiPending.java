package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaksi_pending")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaksiPending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JenisTransaksi jenisTransaksi;

    @Enumerated(EnumType.STRING)
    @Column
    private Simpanan.JenisSimpanan jenisSimpanan;

    @Column(precision = 15, scale = 2)
    private BigDecimal jumlah;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "angsuran_id")
    private AngsuranPinjaman angsuran;

    // Untuk bayar angsuran kelompok
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "angsuran_kelompok_id")
    private AngsuranKelompok angsuranKelompok;

    @Enumerated(EnumType.STRING)
    @Column(name = "metode_bayar")
    private AngsuranKelompok.MetodeBayar metodeBayar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPending status = StatusPending.PENDING;

    @Column(columnDefinition = "TEXT")
    private String catatanAdmin;

    // Path relatif ke file bukti pembayaran (disimpan di app.upload.dir)
    @Column(name = "bukti_bayar", length = 500)
    private String buktiBayar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column
    private LocalDateTime tanggalApproval;

    @Column(nullable = false, updatable = false)
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

    public enum JenisTransaksi {
        SETOR_SIMPANAN, TARIK_SIMPANAN, BAYAR_ANGSURAN, BAYAR_ANGSURAN_KELOMPOK
    }

    public enum StatusPending {
        PENDING, DISETUJUI, DITOLAK
    }
}