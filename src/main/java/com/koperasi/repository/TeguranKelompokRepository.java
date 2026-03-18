package com.koperasi.repository;

import com.koperasi.entity.TeguranKelompok;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeguranKelompokRepository extends JpaRepository<TeguranKelompok, Long> {

    @Query("SELECT t FROM TeguranKelompok t JOIN FETCH t.admin WHERE t.kelompok.id = :kelompokId ORDER BY t.sentAt DESC")
    List<TeguranKelompok> findByKelompokId(@Param("kelompokId") Long kelompokId);
}