package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kelompok")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Kelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String kodeKelompok;

    @Column(nullable = false, length = 100)
    private String namaKelompok;

    @Column(columnDefinition = "TEXT")
    private String deskripsi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusKelompok status = StatusKelompok.AKTIF;

    @OneToMany(mappedBy = "kelompok", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<KelompokAnggota> anggotaList = new ArrayList<>();

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

    public enum StatusKelompok {
        AKTIF,
        TERMINATED   // setelah pinjaman lunas
    }
}