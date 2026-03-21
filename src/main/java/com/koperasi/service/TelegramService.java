package com.koperasi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    @Value("${app.telegram.bot-token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    private String apiUrl() {
        return "https://api.telegram.org/bot" + botToken + "/sendMessage";
    }

    // ─────────────────────────────────────────────────────────
    // Public methods — dipanggil dari service lain
    // ─────────────────────────────────────────────────────────

    /**
     * Notif ke ADMIN: ada pengajuan pinjaman baru
     */
    public void notifPinjamanBaru(String adminChatId, String namaAnggota,
                                  String nomorAnggota, String jumlah, String tujuan) {
        String msg = String.format(
                "🔔 *Pengajuan Pinjaman Baru*\n\n" +
                        "👤 Nama: %s\n" +
                        "🪪 No. Anggota: %s\n" +
                        "💰 Jumlah: Rp %s\n" +
                        "📋 Tujuan: %s\n\n" +
                        "Silakan buka halaman admin untuk memproses.",
                escapeMarkdown(namaAnggota),
                escapeMarkdown(nomorAnggota),
                escapeMarkdown(jumlah),
                escapeMarkdown(tujuan)
        );
        send(adminChatId, msg);
    }

    /**
     * Notif ke MEMBER: pinjaman disetujui
     */
    public void notifPinjamanDisetujui(String memberChatId, String namaAnggota,
                                       String noPinjaman, String jumlah,
                                       String tenor, String angsuranPerBulan) {
        String msg = String.format(
                "✅ *Pinjaman Disetujui*\n\n" +
                        "Halo %s, pengajuan pinjaman Anda telah *DISETUJUI*\\.\n\n" +
                        "📄 No\\. Pinjaman: `%s`\n" +
                        "💰 Jumlah: Rp %s\n" +
                        "📅 Tenor: %s bulan\n" +
                        "🔄 Angsuran/bulan: Rp %s\n\n" +
                        "Silakan cek aplikasi untuk melihat jadwal angsuran\\.",
                escapeMarkdown(namaAnggota),
                escapeMarkdown(noPinjaman),
                escapeMarkdown(jumlah),
                escapeMarkdown(tenor),
                escapeMarkdown(angsuranPerBulan)
        );
        send(memberChatId, msg);
    }

    /**
     * Notif ke MEMBER: pinjaman ditolak
     */
    public void notifPinjamanDitolak(String memberChatId, String namaAnggota,
                                     String jumlah, String catatan) {
        String catatanText = (catatan != null && !catatan.isBlank())
                ? escapeMarkdown(catatan) : "\\-";
        String msg = String.format(
                "❌ *Pinjaman Ditolak*\n\n" +
                        "Halo %s, pengajuan pinjaman Anda *DITOLAK*\\.\n\n" +
                        "💰 Jumlah: Rp %s\n" +
                        "📝 Catatan Admin: %s\n\n" +
                        "Silakan hubungi admin untuk informasi lebih lanjut\\.",
                escapeMarkdown(namaAnggota),
                escapeMarkdown(jumlah),
                catatanText
        );
        send(memberChatId, msg);
    }

    /**
     * Notif ke MEMBER: setor/tarik/bayar angsuran disetujui
     */
    public void notifTransaksiDisetujui(String memberChatId, String namaAnggota,
                                        String jenisTransaksi, String jumlah) {
        String msg = String.format(
                "✅ *Transaksi Disetujui*\n\n" +
                        "Halo %s, pengajuan *%s* Anda telah *DISETUJUI*\\.\n\n" +
                        "💰 Jumlah: Rp %s\n\n" +
                        "Terima kasih telah menggunakan layanan Koperasi Leyangan\\.",
                escapeMarkdown(namaAnggota),
                escapeMarkdown(jenisTransaksi),
                escapeMarkdown(jumlah)
        );
        send(memberChatId, msg);
    }

    /**
     * Notif ke MEMBER: setor/tarik/bayar angsuran ditolak
     */
    public void notifTransaksiDitolak(String memberChatId, String namaAnggota,
                                      String jenisTransaksi, String jumlah,
                                      String catatan) {
        String catatanText = (catatan != null && !catatan.isBlank())
                ? escapeMarkdown(catatan) : "\\-";
        String msg = String.format(
                "❌ *Transaksi Ditolak*\n\n" +
                        "Halo %s, pengajuan *%s* Anda *DITOLAK*\\.\n\n" +
                        "💰 Jumlah: Rp %s\n" +
                        "📝 Catatan Admin: %s\n\n" +
                        "Silakan hubungi admin untuk informasi lebih lanjut\\.",
                escapeMarkdown(namaAnggota),
                escapeMarkdown(jenisTransaksi),
                escapeMarkdown(jumlah),
                catatanText
        );
        send(memberChatId, msg);
    }

    // ─────────────────────────────────────────────────────────
    // Core send
    // ─────────────────────────────────────────────────────────

    public void send(String chatId, String text) {
        if (chatId == null || chatId.isBlank()) {
            log.debug("[Telegram] Chat ID kosong, skip kirim notifikasi");
            return;
        }
        if (botToken == null || botToken.isBlank() || botToken.startsWith("GANTI") || botToken.startsWith("ISIAN")) {
            log.warn("[Telegram] Bot token belum dikonfigurasi");
            return;
        }

        // Deteksi apakah pesan pakai MarkdownV2 (ada karakter * atau `)
        boolean useMarkdown = text.contains("*") || text.contains("`") || text.contains("\\.");
        Map<String, Object> payload = useMarkdown
                ? Map.of("chat_id", chatId, "text", text, "parse_mode", "MarkdownV2")
                : Map.of("chat_id", chatId, "text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl(), HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            log.info("[Telegram] Terkirim ke chat_id {}: {}", chatId, response.getStatusCode());
        } catch (Exception e) {
            log.error("[Telegram] Gagal kirim ke chat_id {}: {}", chatId, e.getMessage());
            // Tidak throw — gagal kirim Telegram tidak boleh ganggu transaksi utama
        }
    }

    /**
     * Escape karakter spesial MarkdownV2 Telegram
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
}