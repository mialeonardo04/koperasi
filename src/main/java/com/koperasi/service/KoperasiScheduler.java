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

    /**
     * Setiap hari jam 08:00 — kirim reminder H-3 sebelum jatuh tempo
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void reminderAngsuranH3() {
        log.info("[Scheduler] Kirim reminder angsuran H-3...");
        try {
            kelompokService.kirimReminderJatuhTempo(3);
        } catch (Exception e) {
            log.error("[Scheduler] Error reminder H-3: {}", e.getMessage());
        }
    }

    /**
     * Setiap hari jam 08:00 — kirim reminder H-1 sebelum jatuh tempo
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void reminderAngsuranH1() {
        log.info("[Scheduler] Kirim reminder angsuran H-1...");
        try {
            kelompokService.kirimReminderJatuhTempo(1);
        } catch (Exception e) {
            log.error("[Scheduler] Error reminder H-1: {}", e.getMessage());
        }
    }
}