package com.koperasi.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurasi Spring Cache untuk mengurangi query ke database.
 * Cache di-invalidate otomatis saat data berubah via @CacheEvict.
 *
 * Cache yang tersedia:
 * - dashboard-admin    : Data dashboard admin (TTL manual, evict saat transaksi baru)
 * - rekap-simpanan     : Rekap simpanan semua member
 * - member-bebas       : Daftar member yang belum di kelompok
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "dashboard-admin",
                "rekap-simpanan",
                "member-bebas"
        );
    }
}