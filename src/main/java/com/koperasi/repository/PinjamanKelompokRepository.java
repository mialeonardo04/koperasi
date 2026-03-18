package com.koperasi.repository;

import com.koperasi.entity.PinjamanKelompok;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PinjamanKelompokRepository extends JpaRepository<PinjamanKelompok, Long> {

    @Query("SELECT p FROM PinjamanKelompok p JOIN FETCH p.kelompok JOIN FETCH p.pengaju ORDER BY p.createdAt DESC")
    Page<PinjamanKelompok> findAllWithKelompok(Pageable pageable);

    @Query("SELECT p FROM PinjamanKelompok p JOIN FETCH p.kelompok JOIN FETCH p.pengaju WHERE p.status = :status")
    Page<PinjamanKelompok> findByStatus(@Param("status") PinjamanKelompok.StatusPinjaman status, Pageable pageable);

    @Query("SELECT p FROM PinjamanKelompok p JOIN FETCH p.kelompok JOIN FETCH p.pengaju WHERE p.kelompok.id = :kelompokId")
    List<PinjamanKelompok> findByKelompokId(@Param("kelompokId") Long kelompokId);

    @Query("SELECT p FROM PinjamanKelompok p JOIN FETCH p.kelompok JOIN FETCH p.pengaju WHERE p.kelompok.id = :kelompokId AND p.status IN ('PENDING','DISETUJUI')")
    Optional<PinjamanKelompok> findActivePinjamanByKelompokId(@Param("kelompokId") Long kelompokId);

    @Query("SELECT COUNT(p) FROM PinjamanKelompok p WHERE p.status = 'PENDING'")
    Long countPending();

    @Query("SELECT COUNT(p) FROM PinjamanKelompok p WHERE p.status = 'DISETUJUI'")
    Long countAktif();

    @Query("SELECT COALESCE(SUM(p.jumlahPinjaman), 0) FROM PinjamanKelompok p WHERE p.status = 'DISETUJUI'")
    BigDecimal getTotalPinjamanAktif();

    @Query("SELECT COALESCE(SUM(p.sisaPinjaman), 0) FROM PinjamanKelompok p WHERE p.status = 'DISETUJUI'")
    BigDecimal getTotalSisaPinjaman();
}