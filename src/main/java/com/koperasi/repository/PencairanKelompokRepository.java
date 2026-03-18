package com.koperasi.repository;

import com.koperasi.entity.PencairanKelompok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PencairanKelompokRepository extends JpaRepository<PencairanKelompok, Long> {

    @Query("""
        SELECT p FROM PencairanKelompok p
        JOIN FETCH p.user
        WHERE p.pinjamanKelompok.id = :pinjamanId
        ORDER BY p.createdAt DESC
        """)
    List<PencairanKelompok> findByPinjamanKelompokId(@Param("pinjamanId") Long pinjamanId);

    @Query("""
        SELECT p FROM PencairanKelompok p
        JOIN FETCH p.user
        JOIN FETCH p.pinjamanKelompok pk
        WHERE pk.kelompok.id = :kelompokId AND p.status = 'PENDING'
        ORDER BY p.createdAt ASC
        """)
    List<PencairanKelompok> findPendingByKelompokId(@Param("kelompokId") Long kelompokId);

    @Query("""
        SELECT COALESCE(SUM(p.jumlah), 0)
        FROM PencairanKelompok p
        WHERE p.pinjamanKelompok.id = :pinjamanId
        AND p.status = 'DISETUJUI'
        """)
    BigDecimal sumCairanDisetujui(@Param("pinjamanId") Long pinjamanId);

    @Query("""
        SELECT COALESCE(SUM(p.jumlah), 0)
        FROM PencairanKelompok p
        WHERE p.pinjamanKelompok.id = :pinjamanId
        AND p.user.id = :userId
        AND p.status = 'DISETUJUI'
        """)
    BigDecimal sumCairanByUser(@Param("pinjamanId") Long pinjamanId, @Param("userId") Long userId);

    long countByPinjamanKelompokIdAndStatus(Long pinjamanKelompokId, PencairanKelompok.StatusPencairan status);
}