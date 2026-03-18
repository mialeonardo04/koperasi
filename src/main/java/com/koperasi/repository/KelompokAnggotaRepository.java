package com.koperasi.repository;

import com.koperasi.entity.Kelompok;
import com.koperasi.entity.KelompokAnggota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KelompokAnggotaRepository extends JpaRepository<KelompokAnggota, Long> {

    List<KelompokAnggota> findByKelompokId(Long kelompokId);

    // Cek apakah user sudah ada di kelompok aktif
    @Query("""
        SELECT ka FROM KelompokAnggota ka
        JOIN FETCH ka.kelompok k
        WHERE ka.user.id = :userId AND k.status = 'AKTIF'
        """)
    Optional<KelompokAnggota> findActiveKelompokByUserId(@Param("userId") Long userId);

    boolean existsByKelompokIdAndUserId(Long kelompokId, Long userId);

    long countByKelompokId(Long kelompokId);

    Optional<KelompokAnggota> findByKelompokIdAndUserId(Long kelompokId, Long userId);

    /** Ambil semua user_id yang terikat kelompok AKTIF */
    @Query("SELECT ka.user.id FROM KelompokAnggota ka WHERE ka.kelompok.status = 'AKTIF'")
    List<Long> findUserIdsTerikatKelompokAktif();
}