package com.koperasi.repository;

import com.koperasi.entity.AngsuranPinjaman;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AngsuranPinjamanRepository extends JpaRepository<AngsuranPinjaman, Long> {

    List<AngsuranPinjaman> findByPinjamanIdOrderByPeriodeKe(Long pinjamanId);

    Optional<AngsuranPinjaman> findByPinjamanIdAndPeriodeKe(Long pinjamanId, Integer periodeKe);

    @Query("SELECT a FROM AngsuranPinjaman a WHERE a.pinjaman.user.id = :userId " +
            "AND a.status = 'BELUM_BAYAR' AND a.tanggalJatuhTempo < :today ORDER BY a.tanggalJatuhTempo")
    List<AngsuranPinjaman> findAngsuranTerlambat(@Param("userId") Long userId,
                                                 @Param("today") LocalDate today);

    @Query("SELECT a FROM AngsuranPinjaman a WHERE a.pinjaman.id = :pinjamanId " +
            "AND a.status = 'BELUM_BAYAR' ORDER BY a.periodeKe ASC LIMIT 1")
    Optional<AngsuranPinjaman> findNextAngsuran(@Param("pinjamanId") Long pinjamanId);

    @Query("SELECT COALESCE(SUM(a.jumlahAngsuran), 0) FROM AngsuranPinjaman a " +
            "WHERE a.pinjaman.user.id = :userId AND a.status = 'SUDAH_BAYAR'")
    BigDecimal getTotalAngsuranBayarByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM AngsuranPinjaman a WHERE a.status = 'BELUM_BAYAR' " +
            "AND a.tanggalJatuhTempo < :today")
    Long countAngsuranJatuhTempo(@Param("today") LocalDate today);
}