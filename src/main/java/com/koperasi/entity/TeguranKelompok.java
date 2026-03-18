package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teguran_kelompok")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TeguranKelompok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kelompok_id", nullable = false)
    private Kelompok kelompok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pesan;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}