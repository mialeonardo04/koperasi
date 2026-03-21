package com.koperasi.repository;

import com.koperasi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNomorAnggota(String nomorAnggota);
    boolean existsByEmail(String email);
    boolean existsByNomorAnggota(String nomorAnggota);

    // Ambil semua member tanpa filter (dipakai saat search kosong / null)
    @Query("SELECT u FROM User u WHERE u.role = 'MEMBER'")
    Page<User> findAllMembers(Pageable pageable);

    // Cari member berdasarkan nama atau nomor anggota (native query menghindari bug bytea)
    @Query(value = "SELECT * FROM users WHERE role = 'MEMBER' AND " +
            "(LOWER(nama_lengkap) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(nomor_anggota) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(*) FROM users WHERE role = 'MEMBER' AND " +
                    "(LOWER(nama_lengkap) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(nomor_anggota) LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<User> findAllMembersBySearch(@Param("search") String search, Pageable pageable);
    /**
     * OPTIMASI: Ambil member yang tidak terikat kelompok AKTIF dalam 1 query
     * Menggantikan findAll() + filter di Java
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role = 'MEMBER' AND u.status = 'AKTIF'
        AND u.id NOT IN (
            SELECT ka.user.id FROM KelompokAnggota ka
            WHERE ka.kelompok.status = 'AKTIF'
        )
        ORDER BY u.nomorAnggota
        """)
    List<User> findMemberBebasKelompokAktif();
}