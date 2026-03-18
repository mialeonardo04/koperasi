package com.koperasi.repository;

import com.koperasi.entity.Pinjaman;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PinjamanRepository extends JpaRepository<Pinjaman, Long> {

    @Query("SELECT p FROM Pinjaman p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Pinjaman> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    Optional<Pinjaman> findByNoPinjaman(String noPinjaman);

    @Query("SELECT p FROM Pinjaman p JOIN FETCH p.user WHERE p.user.id = :userId AND p.status = :status")
    List<Pinjaman> findByUserIdAndStatus(@Param("userId") Long userId,
                                         @Param("status") Pinjaman.StatusPinjaman status);

    @Query(value = "SELECT p FROM Pinjaman p JOIN FETCH p.user u WHERE " +
            "(:userId IS NULL OR u.id = :userId) AND " +
            "(:status IS NULL OR p.status = :status) " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Pinjaman p WHERE " +
                    "(:userId IS NULL OR p.user.id = :userId) AND " +
                    "(:status IS NULL OR p.status = :status)")
    Page<Pinjaman> findWithFilter(@Param("userId") Long userId,
                                  @Param("status") Pinjaman.StatusPinjaman status,
                                  Pageable pageable);

    // JOIN FETCH user untuk getRekapPinjamanAllMember di ReportService
    @Query("SELECT p FROM Pinjaman p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    List<Pinjaman> findAllWithUser();

    @Query("SELECT COUNT(p) FROM Pinjaman p WHERE p.status = 'DISETUJUI'")
    Long countAktif();

    @Query("SELECT COUNT(p) FROM Pinjaman p WHERE p.status = 'PENDING'")
    Long countPending();

    @Query("SELECT COALESCE(SUM(p.jumlahPinjaman), 0) FROM Pinjaman p WHERE p.status IN ('DISETUJUI', 'MACET')")
    BigDecimal getTotalPinjamanAktif();

    @Query("SELECT COALESCE(SUM(p.sisaPinjaman), 0) FROM Pinjaman p WHERE p.status IN ('DISETUJUI', 'MACET')")
    BigDecimal getTotalSisaPinjaman();

    boolean existsByUserIdAndStatusIn(Long userId, List<Pinjaman.StatusPinjaman> statuses);
}