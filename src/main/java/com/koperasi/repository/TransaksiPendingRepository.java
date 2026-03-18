package com.koperasi.repository;

import com.koperasi.entity.TransaksiPending;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaksiPendingRepository extends JpaRepository<TransaksiPending, Long> {

    // Semua pending milik satu user
    @Query("SELECT t FROM TransaksiPending t JOIN FETCH t.user WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    Page<TransaksiPending> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // Semua pending (admin view) dengan filter status opsional
    @Query("SELECT t FROM TransaksiPending t JOIN FETCH t.user WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<TransaksiPending> findAllPending();

    // Semua transaksi dengan filter status (admin)
    @Query("SELECT t FROM TransaksiPending t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    Page<TransaksiPending> findAllWithUser(Pageable pageable);

    @Query("SELECT t FROM TransaksiPending t JOIN FETCH t.user WHERE t.status = :status ORDER BY t.createdAt DESC")
    Page<TransaksiPending> findByStatus(@Param("status") TransaksiPending.StatusPending status, Pageable pageable);

    // Count untuk badge notifikasi admin
    @Query("SELECT COUNT(t) FROM TransaksiPending t WHERE t.status = 'PENDING'")
    Long countPending();

    // Cek apakah angsuran sudah ada pending yang belum diproses
    @Query("SELECT COUNT(t) FROM TransaksiPending t WHERE t.angsuran.id = :angsuranId AND t.status = 'PENDING'")
    Long countPendingByAngsuranId(@Param("angsuranId") Long angsuranId);

    // Cek apakah sudah ada pending setor/tarik untuk jenis simpanan yang sama
    @Query("SELECT COUNT(t) FROM TransaksiPending t WHERE t.user.id = :userId " +
            "AND t.jenisSimpanan = :jenisSimpanan AND t.jenisTransaksi = :jenis AND t.status = 'PENDING'")
    Long countPendingByUserAndJenis(@Param("userId") Long userId,
                                    @Param("jenisSimpanan") com.koperasi.entity.Simpanan.JenisSimpanan jenisSimpanan,
                                    @Param("jenis") TransaksiPending.JenisTransaksi jenis);
}