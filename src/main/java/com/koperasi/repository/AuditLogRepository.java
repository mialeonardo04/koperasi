package com.koperasi.repository;

import com.koperasi.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.user ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllWithUser(Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, String entityId, Pageable pageable);
}
