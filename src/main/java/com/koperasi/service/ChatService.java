package com.koperasi.service;

import com.koperasi.entity.Simpanan;
import com.koperasi.entity.User;
import com.koperasi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository              userRepository;
    private final SimpananRepository          simpananRepository;
    private final PinjamanKelompokRepository  pinjamanKelompokRepository;
    private final KelompokAnggotaRepository   kelompokAnggotaRepository;
    private final TransaksiPendingRepository  pendingRepository;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";
    private static final int    MAX_HISTORY = 10;

    // History per user: list of {role, content}
    private final Map<Long, List<Map<String, String>>> historyMap = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public String chat(Long userId, String pesan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        String systemPrompt = buildSystemPrompt(user);
        List<Map<String, String>> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());

        // Tambah pesan user
        history.add(Map.of("role", "user", "content", pesan));

        // Batasi history
        if (history.size() > MAX_HISTORY * 2) {
            history.subList(0, 2).clear();
        }

        String balasan = callGroq(systemPrompt, history);

        // Tambah balasan ke history
        history.add(Map.of("role", "assistant", "content", balasan));

        return balasan;
    }

    public void resetHistory(Long userId) {
        historyMap.remove(userId);
    }

    @Transactional(readOnly = true)
    private String buildSystemPrompt(User user) {
        BigDecimal pokok    = simpananRepository.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.POKOK);
        BigDecimal wajib    = simpananRepository.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.WAJIB);
        BigDecimal sukarela = simpananRepository.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.SUKARELA);

        long pengajuanMenunggu = pendingRepository.countByUserIdAndStatus(
                user.getId(), com.koperasi.entity.TransaksiPending.StatusPending.PENDING);

        String infoPinjaman = "Tidak ada pinjaman kelompok aktif";
        var keanggotaan = kelompokAnggotaRepository.findByUserId(user.getId());
        for (var ka : keanggotaan) {
            if (ka.getKelompok().getStatus() == com.koperasi.entity.Kelompok.StatusKelompok.AKTIF) {
                var pkOpt = pinjamanKelompokRepository.findActivePinjamanByKelompokId(ka.getKelompok().getId());
                if (pkOpt.isPresent()) {
                    var pk = pkOpt.get();
                    if (pk.getStatus() == com.koperasi.entity.PinjamanKelompok.StatusPinjaman.DISETUJUI) {
                        long sudahBayar = pk.getAngsuranList() == null ? 0 :
                                pk.getAngsuranList().stream()
                                        .filter(a -> a.getStatus() == com.koperasi.entity.AngsuranKelompok.StatusAngsuran.SUDAH_BAYAR)
                                        .count();
                        infoPinjaman = String.format(
                                "Kelompok: %s | Pinjaman: Rp %,.0f | Sisa: Rp %,.0f | Angsuran/bln: Rp %,.0f | Tenor: %d bulan | Sudah bayar: %d angsuran | Jatuh tempo: %s",
                                ka.getKelompok().getNamaKelompok(),
                                pk.getJumlahPinjaman(), pk.getSisaPinjaman(),
                                pk.getAngsuranPerBulan(), pk.getTenorBulan(),
                                sudahBayar, pk.getTanggalJatuhTempo()
                        );
                        break;
                    }
                }
            }
        }

        return String.format(
                "Kamu adalah asisten virtual Koperasi Simpan Pinjam Leyangan yang ramah dan membantu.\n" +
                        "Jawab dalam Bahasa Indonesia yang sopan dan singkat.\n" +
                        "Kamu hanya boleh membahas topik seputar koperasi, simpanan, pinjaman, dan layanan koperasi.\n" +
                        "Jika ditanya hal di luar itu, tolak dengan sopan.\n\n" +
                        "DATA ANGGOTA SAAT INI:\n" +
                        "Nama          : %s\n" +
                        "No. Anggota   : %s\n" +
                        "Tanggal       : %s\n\n" +
                        "SIMPANAN:\n" +
                        "- Simpanan Pokok   : Rp %,.0f\n" +
                        "- Simpanan Wajib   : Rp %,.0f\n" +
                        "- Simpanan Sukarela: Rp %,.0f\n" +
                        "- Total Simpanan   : Rp %,.0f\n\n" +
                        "PINJAMAN KELOMPOK: %s\n\n" +
                        "PENGAJUAN MENUNGGU: %d transaksi sedang menunggu persetujuan admin\n\n" +
                        "LAYANAN YANG TERSEDIA:\n" +
                        "- Setor simpanan (Pokok/Wajib/Sukarela)\n" +
                        "- Tarik simpanan Sukarela/Wajib\n" +
                        "- Pinjaman melalui kelompok (min 5 anggota, max Rp 3.000.000, max 10 bulan)\n" +
                        "- Bayar angsuran kelompok (via transfer atau potong simpanan)\n" +
                        "- Cairkan dana dari pool pinjaman kelompok",
                user.getNamaLengkap(), user.getNomorAnggota(), LocalDate.now(),
                pokok, wajib, sukarela, pokok.add(wajib).add(sukarela),
                infoPinjaman, pengajuanMenunggu
        );
    }

    @SuppressWarnings("unchecked")
    private String callGroq(String systemPrompt, List<Map<String, String>> history) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            // Bangun messages: system + history
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.addAll(history);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("max_tokens", 1024);
            body.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("Error memanggil Groq API: {}", e.getMessage());
        }
        return "Maaf, saya sedang mengalami gangguan. Silakan coba lagi nanti.";
    }
}