package com.koperasi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kelompok_anggota",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kelompok_id", "user_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KelompokAnggota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kelompok_id", nullable = false)
    private Kelompok kelompok;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}