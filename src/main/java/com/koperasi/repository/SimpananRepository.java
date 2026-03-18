package com.koperasi.repository;

import com.koperasi.entity.Simpanan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SimpananRepository extends JpaRepository<Simpanan, Long> {

    @Query("SELECT s FROM Simpanan s JOIN FETCH s.user WHERE s.user.id = :userId ORDER BY s.tanggalTransaksi DESC")
    Page<Simpanan> findByUserIdOrderByTanggalTransaksiDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM Simpanan s JOIN FETCH s.user WHERE s.user.id = :userId AND s.jenis = :jenis ORDER BY s.tanggalTransaksi DESC")
    List<Simpanan> findByUserIdAndJenis(@Param("userId") Long userId,
                                        @Param("jenis") Simpanan.JenisSimpanan jenis);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.tipe = 'SETOR' THEN s.jumlah ELSE -s.jumlah END), 0) " +
            "FROM Simpanan s WHERE s.user.id = :userId AND s.jenis = :jenis")
    BigDecimal getSaldoByUserAndJenis(@Param("userId") Long userId,
                                      @Param("jenis") Simpanan.JenisSimpanan jenis);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.tipe = 'SETOR' THEN s.jumlah ELSE -s.jumlah END), 0) " +
            "FROM Simpanan s WHERE s.user.id = :userId")
    BigDecimal getTotalSimpananByUser(@Param("userId") Long userId);

    // ── Semua transaksi (admin, tanpa filter) ──
    @Query("SELECT s FROM Simpanan s JOIN FETCH s.user ORDER BY s.tanggalTransaksi DESC")
    Page<Simpanan> findAllWithUser(Pageable pageable);

    // ── Filter by userId saja ──
    @Query("SELECT s FROM Simpanan s JOIN FETCH s.user u WHERE u.id = :userId ORDER BY s.tanggalTransaksi DESC")
    Page<Simpanan> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // ── Filter by rentang tanggal saja ──
    @Query("SELECT s FROM Simpanan s JOIN FETCH s.user WHERE s.tanggalTransaksi >= :dari AND s.tanggalTransaksi <= :sampai ORDER BY s.tanggalTransaksi DESC")
    Page<Simpanan> findByDateRange(@Param("dari") LocalDateTime dari,
                                   @Param("sampai") LocalDateTime sampai,
                                   Pageable pageable);

    // ── Filter by userId + rentang tanggal ──
    @Query("SELECT s FROM Simpanan s JOIN FETCH s.user u WHERE u.id = :userId AND s.tanggalTransaksi >= :dari AND s.tanggalTransaksi <= :sampai ORDER BY s.tanggalTransaksi DESC")
    Page<Simpanan> findByUserIdAndDateRange(@Param("userId") Long userId,
                                            @Param("dari") LocalDateTime dari,
                                            @Param("sampai") LocalDateTime sampai,
                                            Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.jumlah), 0) FROM Simpanan s WHERE s.tipe = 'SETOR'")
    BigDecimal getTotalSetoranSemua();

    @Query("SELECT COALESCE(SUM(s.jumlah), 0) FROM Simpanan s WHERE s.tipe = 'TARIK'")
    BigDecimal getTotalPenarikanSemua();
}