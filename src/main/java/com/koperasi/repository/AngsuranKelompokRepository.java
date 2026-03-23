package com.koperasi.repository;

import com.koperasi.entity.AngsuranKelompok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AngsuranKelompokRepository extends JpaRepository<AngsuranKelompok, Long> {

    List<AngsuranKelompok> findByPinjamanKelompokId(Long pinjamanKelompokId);

    // Angsuran yang sudah jatuh tempo tapi belum bayar (untuk cek terlambat)
    @Query("""
        SELECT a FROM AngsuranKelompok a
        JOIN FETCH a.pinjamanKelompok p
        JOIN FETCH p.kelompok k
        WHERE a.status = 'BELUM_BAYAR'
        AND a.tanggalJatuhTempo < :today
        """)
    List<AngsuranKelompok> findTerlambat(@Param("today") LocalDate today);

    // Angsuran yang jatuh tempo dalam N hari ke depan (untuk notif reminder)
    @Query("""
        SELECT a FROM AngsuranKelompok a
        JOIN FETCH a.pinjamanKelompok p
        JOIN FETCH p.kelompok k
        JOIN FETCH k.anggotaList ka
        JOIN FETCH ka.user u
        WHERE a.status = 'BELUM_BAYAR'
        AND a.tanggalJatuhTempo = :targetDate
        """)
    List<AngsuranKelompok> findJatuhTempoOnDate(@Param("targetDate") LocalDate targetDate);

    // Cek ada pending bayar untuk angsuran ini
    @Query("""
        SELECT COUNT(t) FROM TransaksiPending t
        WHERE t.angsuranKelompok.id = :angsuranId
        AND t.status = 'PENDING'
        """)
    long countPendingByAngsuranKelompokId(@Param("angsuranId") Long angsuranId);
}