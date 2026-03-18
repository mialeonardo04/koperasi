package com.koperasi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KoperasiScheduler {

    private final KelompokService kelompokService;

    /**
     * Setiap hari jam 08:00 — cek angsuran kelompok yang terlambat
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void cekAngsuranTerlambat() {
        log.info("[Scheduler] Cek angsuran kelompok terlambat...");
        try {
            kelompokService.cekAngsuranTerlambat();
        } catch (Exception e) {
            log.error("[Scheduler] Error cek angsuran terlambat: {}", e.getMessage());
        }
    }
}