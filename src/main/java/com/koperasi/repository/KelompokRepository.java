package com.koperasi.repository;

import com.koperasi.entity.Kelompok;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KelompokRepository extends JpaRepository<Kelompok, Long> {

    @Query("SELECT k FROM Kelompok k JOIN FETCH k.leader WHERE k.status = 'AKTIF'")
    Page<Kelompok> findAllAktif(Pageable pageable);

    @Query("SELECT k FROM Kelompok k JOIN FETCH k.leader")
    Page<Kelompok> findAllWithLeader(Pageable pageable);

    Optional<Kelompok> findByKodeKelompok(String kodeKelompok);

    boolean existsByKodeKelompok(String kodeKelompok);

    @Query("SELECT COUNT(k) FROM Kelompok k WHERE k.status = 'AKTIF'")
    Long countAktif();
}