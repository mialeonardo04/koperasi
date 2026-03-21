package com.koperasi.service;

import com.koperasi.dto.KelompokDto;
import com.koperasi.entity.*;
import com.koperasi.repository.PencairanKelompokRepository;
import com.koperasi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KelompokService {

    private final KelompokRepository           kelompokRepo;
    private final KelompokAnggotaRepository    anggotaRepo;
    private final PinjamanKelompokRepository   pinjamanRepo;
    private final AngsuranKelompokRepository   angsuranRepo;
    private final TeguranKelompokRepository    teguranRepo;
    private final UserRepository               userRepo;
    private final SimpananRepository           simpananRepo;
    private final TransaksiPendingRepository   pendingRepo;
    private final TelegramService              telegramService;
    private final PencairanKelompokRepository  pencairanRepo;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    // ─────────────────────────────────────────────────────────
    // KELOMPOK — CRUD
    // ─────────────────────────────────────────────────────────

    @Transactional
    public KelompokDto.KelompokResponse buatKelompok(Long userId, KelompokDto.BuatKelompokRequest req) {
        User user = getUser(userId);

        // Cek user sudah di kelompok aktif
        anggotaRepo.findActiveKelompokByUserId(userId).ifPresent(ka -> {
            throw new IllegalStateException(
                    "Anda sudah tergabung dalam kelompok aktif: " + ka.getKelompok().getNamaKelompok() +
                            ". Selesaikan pinjaman terlebih dahulu sebelum membuat kelompok baru.");
        });

        String kode = generateKodeKelompok();
        Kelompok kelompok = Kelompok.builder()
                .kodeKelompok(kode)
                .namaKelompok(req.getNamaKelompok())
                .deskripsi(req.getDeskripsi())
                .leader(user)
                .status(Kelompok.StatusKelompok.AKTIF)
                .build();
        kelompok = kelompokRepo.save(kelompok);

        // Leader otomatis jadi anggota pertama
        KelompokAnggota ka = KelompokAnggota.builder()
                .kelompok(kelompok)
                .user(user)
                .build();
        anggotaRepo.save(ka);

        log.info("Kelompok {} dibuat oleh user {}", kode, userId);
        return mapToResponse(kelompok, false);
    }

    @Transactional
    @CacheEvict(value = {"member-bebas", "dashboard-admin"}, allEntries = true)
    public KelompokDto.KelompokResponse tambahAnggota(Long kelompokId, Long leaderId, KelompokDto.TambahAnggotaRequest req) {
        Kelompok kelompok = getKelompok(kelompokId);

        // Hanya leader yang bisa tambah anggota
        if (!kelompok.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("Hanya leader yang bisa menambahkan anggota");
        }

        if (kelompok.getStatus() == Kelompok.StatusKelompok.TERMINATED) {
            throw new IllegalStateException("Kelompok sudah terminated");
        }

        User userBaru = getUser(req.getUserId());

        // Cek sudah di kelompok aktif lain
        anggotaRepo.findActiveKelompokByUserId(req.getUserId()).ifPresent(ka -> {
            throw new IllegalStateException(
                    userBaru.getNamaLengkap() + " sudah tergabung dalam kelompok: " +
                            ka.getKelompok().getNamaKelompok());
        });

        if (anggotaRepo.existsByKelompokIdAndUserId(kelompokId, req.getUserId())) {
            throw new IllegalStateException("Anggota sudah tergabung dalam kelompok ini");
        }

        KelompokAnggota ka = KelompokAnggota.builder()
                .kelompok(kelompok)
                .user(userBaru)
                .build();
        anggotaRepo.save(ka);

        log.info("User {} ditambahkan ke kelompok {}", req.getUserId(), kelompokId);
        return mapToResponse(kelompok, true);
    }

    @Transactional
    public KelompokDto.KelompokResponse keluarKelompok(Long kelompokId, Long userId) {
        Kelompok kelompok = getKelompok(kelompokId);

        if (kelompok.getLeader().getId().equals(userId)) {
            throw new IllegalStateException("Leader tidak bisa keluar dari kelompok. Bubarkan kelompok atau pindahkan leadership terlebih dahulu.");
        }

        // Cek ada pinjaman aktif
        pinjamanRepo.findActivePinjamanByKelompokId(kelompokId).ifPresent(p -> {
            throw new IllegalStateException("Tidak bisa keluar — kelompok masih memiliki pinjaman aktif");
        });

        KelompokAnggota ka = anggotaRepo.findByKelompokIdAndUserId(kelompokId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Anda bukan anggota kelompok ini"));
        anggotaRepo.delete(ka);
        return mapToResponse(kelompok, true);
    }

    @Transactional
    public KelompokDto.KelompokResponse bubarkanKelompok(Long kelompokId, Long leaderId) {
        Kelompok kelompok = getKelompok(kelompokId);

        // Hanya leader yang bisa membubarkan
        if (!kelompok.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("Hanya pembuat kelompok yang bisa membubarkan kelompok");
        }

        if (kelompok.getStatus() == Kelompok.StatusKelompok.TERMINATED) {
            throw new IllegalStateException("Kelompok sudah dibubarkan");
        }

        // Cek apakah ada pinjaman PENDING atau DISETUJUI
        pinjamanRepo.findActivePinjamanByKelompokId(kelompokId).ifPresent(p -> {
            if (p.getStatus() == PinjamanKelompok.StatusPinjaman.PENDING) {
                throw new IllegalStateException(
                        "Tidak bisa membubarkan kelompok — masih ada pengajuan pinjaman yang menunggu persetujuan admin.");
            }
            if (p.getStatus() == PinjamanKelompok.StatusPinjaman.DISETUJUI) {
                throw new IllegalStateException(
                        "Tidak bisa membubarkan kelompok — masih ada pinjaman aktif yang belum lunas.");
            }
        });

        // Aman untuk dibubarkan
        kelompok.setStatus(Kelompok.StatusKelompok.TERMINATED);
        kelompokRepo.save(kelompok);

        log.info("Kelompok {} DIBUBARKAN oleh leader {}", kelompok.getKodeKelompok(), leaderId);
        return mapToResponse(kelompok, true);
    }

    @Transactional(readOnly = true)
    public KelompokDto.KelompokResponse getKelompokDetail(Long kelompokId) {
        return mapToResponse(getKelompok(kelompokId), true);
    }

    @Transactional(readOnly = true)
    public KelompokDto.KelompokResponse getKelompokSaya(Long userId) {
        KelompokAnggota ka = anggotaRepo.findActiveKelompokByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Anda belum tergabung dalam kelompok aktif"));
        // Fetch ulang dengan ID agar tidak kena lazy loading issue
        Kelompok kelompok = kelompokRepo.findById(ka.getKelompok().getId())
                .orElseThrow(() -> new IllegalArgumentException("Kelompok tidak ditemukan"));
        return mapToResponse(kelompok, true);
    }

    @Transactional(readOnly = true)
    public Page<KelompokDto.KelompokResponse> getAllKelompok(Pageable pageable) {
        return kelompokRepo.findAllWithLeader(pageable).map(k -> mapToResponse(k, false));
    }

    // ─────────────────────────────────────────────────────────
    // PINJAMAN KELOMPOK
    // ─────────────────────────────────────────────────────────

    @Transactional
    public KelompokDto.PinjamanKelompokResponse ajukanPinjaman(Long userId, Long kelompokId,
                                                               KelompokDto.AjukanPinjamanRequest req) {
        User user     = getUser(userId);
        Kelompok kelompok = getKelompok(kelompokId);

        // Cek user adalah anggota kelompok ini
        if (!anggotaRepo.existsByKelompokIdAndUserId(kelompokId, userId)) {
            throw new IllegalStateException("Anda bukan anggota kelompok ini");
        }

        if (kelompok.getStatus() == Kelompok.StatusKelompok.TERMINATED) {
            throw new IllegalStateException("Kelompok sudah terminated");
        }

        // Cek minimal anggota (min 2)
        long jumlahAnggota = anggotaRepo.countByKelompokId(kelompokId);
        if (jumlahAnggota < 5) {
            throw new IllegalStateException("Kelompok harus memiliki minimal 5 anggota untuk mengajukan pinjaman");
        }

        // Cek sudah ada pinjaman aktif
        pinjamanRepo.findActivePinjamanByKelompokId(kelompokId).ifPresent(p -> {
            throw new IllegalStateException("Kelompok ini sudah memiliki pinjaman aktif: " + p.getNoPinjaman());
        });

        BigDecimal angsuran = hitungAngsuran(req.getJumlahPinjaman(),
                new BigDecimal("1.5"), req.getTenorBulan());

        PinjamanKelompok pinjaman = PinjamanKelompok.builder()
                .noPinjaman(generateNoPinjaman())
                .kelompok(kelompok)
                .pengaju(user)
                .jumlahPinjaman(req.getJumlahPinjaman())
                .tenorBulan(req.getTenorBulan())
                .angsuranPerBulan(angsuran)
                .sisaPinjaman(req.getJumlahPinjaman())
                .tujuanPinjaman(req.getTujuanPinjaman())
                .status(PinjamanKelompok.StatusPinjaman.PENDING)
                .build();
        pinjaman = pinjamanRepo.save(pinjaman);

        // Notif ke admin
        if (adminChatId != null && !adminChatId.isBlank()) {
            telegramService.send(adminChatId,
                    "🔔 *Pengajuan Pinjaman Kelompok Baru*\n\n" +
                            "👥 Kelompok: " + escMd(kelompok.getNamaKelompok()) + "\n" +
                            "🪪 Kode: `" + kelompok.getKodeKelompok() + "`\n" +
                            "👤 Pengaju: " + escMd(user.getNamaLengkap()) + "\n" +
                            "💰 Jumlah: Rp " + escMd(formatRupiah(req.getJumlahPinjaman())) + "\n" +
                            "📅 Tenor: " + req.getTenorBulan() + " bulan\n" +
                            "📋 Tujuan: " + escMd(req.getTujuanPinjaman() != null ? req.getTujuanPinjaman() : "-") + "\n\n" +
                            "Silakan buka halaman admin untuk memproses\\.");
        }

        log.info("Pengajuan pinjaman kelompok {} oleh kelompok {}", pinjaman.getNoPinjaman(), kelompokId);
        return mapToPinjamanResponse(pinjaman, false);
    }

    @Transactional
    public KelompokDto.PinjamanKelompokResponse prosesPinjaman(Long pinjamanId,
                                                               boolean disetujui, String keteranganAdmin) {
        PinjamanKelompok pinjaman = pinjamanRepo.findById(pinjamanId)
                .orElseThrow(() -> new IllegalArgumentException("Pinjaman tidak ditemukan"));

        if (pinjaman.getStatus() != PinjamanKelompok.StatusPinjaman.PENDING) {
            throw new IllegalStateException("Pinjaman sudah diproses sebelumnya");
        }

        if (disetujui) {
            BigDecimal bunga   = new BigDecimal("1.5");
            BigDecimal angsuran = hitungAngsuran(pinjaman.getJumlahPinjaman(), bunga, pinjaman.getTenorBulan());

            pinjaman.setBungaPerBulan(bunga);
            pinjaman.setAngsuranPerBulan(angsuran);
            pinjaman.setStatus(PinjamanKelompok.StatusPinjaman.DISETUJUI);
            pinjaman.setTanggalDisetujui(LocalDate.now());
            pinjaman.setTanggalJatuhTempo(LocalDate.now().plusMonths(pinjaman.getTenorBulan()));
            pinjaman.setKeteranganAdmin(keteranganAdmin);
            pinjaman.getAngsuranList().clear();
            pinjaman.getAngsuranList().addAll(generateJadwal(pinjaman));

            // Notif ke semua anggota kelompok
            notifKeAnggotaKelompok(pinjaman.getKelompok(),
                    "✅ *Pinjaman Kelompok Disetujui\\!*\n\n" +
                            "Halo anggota kelompok *" + escMd(pinjaman.getKelompok().getNamaKelompok()) + "*,\n" +
                            "Pinjaman kelompok kalian telah *DISETUJUI*\\.\n\n" +
                            "💰 Jumlah: Rp " + escMd(formatRupiah(pinjaman.getJumlahPinjaman())) + "\n" +
                            "📅 Tenor: " + pinjaman.getTenorBulan() + " bulan\n" +
                            "🔄 Angsuran/bulan: Rp " + escMd(formatRupiah(angsuran)) + "\n" +
                            "📆 Jatuh tempo: " + pinjaman.getTanggalJatuhTempo() + "\n\n" +
                            "Silakan cek aplikasi untuk melihat jadwal angsuran\\.");
        } else {
            pinjaman.setStatus(PinjamanKelompok.StatusPinjaman.DITOLAK);
            pinjaman.setKeteranganAdmin(keteranganAdmin);

            notifKeAnggotaKelompok(pinjaman.getKelompok(),
                    "❌ *Pinjaman Kelompok Ditolak*\n\n" +
                            "Halo anggota kelompok *" + escMd(pinjaman.getKelompok().getNamaKelompok()) + "*,\n" +
                            "Pengajuan pinjaman kelompok *DITOLAK*\\.\n\n" +
                            "📝 Catatan: " + escMd(keteranganAdmin != null ? keteranganAdmin : "-") + "\n\n" +
                            "Silakan hubungi admin untuk informasi lebih lanjut\\.");
        }

        pinjaman = pinjamanRepo.save(pinjaman);
        return mapToPinjamanResponse(pinjaman, true);
    }

    // ─────────────────────────────────────────────────────────
    // BAYAR ANGSURAN KELOMPOK
    // ─────────────────────────────────────────────────────────

    @Transactional
    public TransaksiPending ajukanBayarAngsuran(Long userId, KelompokDto.BayarAngsuranRequest req) {
        User user = getUser(userId);
        AngsuranKelompok angsuran = angsuranRepo.findById(req.getAngsuranId())
                .orElseThrow(() -> new IllegalArgumentException("Angsuran tidak ditemukan"));

        PinjamanKelompok pinjaman = angsuran.getPinjamanKelompok();

        // Cek user adalah anggota kelompok
        if (!anggotaRepo.existsByKelompokIdAndUserId(
                pinjaman.getKelompok().getId(), userId)) {
            throw new IllegalStateException("Anda bukan anggota kelompok ini");
        }

        if (angsuran.getStatus() == AngsuranKelompok.StatusAngsuran.SUDAH_BAYAR) {
            throw new IllegalStateException("Angsuran ini sudah dibayar");
        }

        // Cek belum ada pending untuk angsuran ini
        if (angsuranRepo.countPendingByAngsuranKelompokId(req.getAngsuranId()) > 0) {
            throw new IllegalStateException("Sudah ada pengajuan pembayaran yang menunggu persetujuan admin");
        }

        AngsuranKelompok.MetodeBayar metode = AngsuranKelompok.MetodeBayar.valueOf(
                req.getMetodeBayar() != null ? req.getMetodeBayar() : "TRANSFER");

        // Jika SIMPANAN — cek saldo per jenis
        if (metode == AngsuranKelompok.MetodeBayar.SIMPANAN) {
            Simpanan.JenisSimpanan jenis = req.getJenisSimpanan() != null
                    ? Simpanan.JenisSimpanan.valueOf(req.getJenisSimpanan())
                    : Simpanan.JenisSimpanan.SUKARELA;
            BigDecimal saldoJenis = simpananRepo.getSaldoByUserAndJenis(userId, jenis);
            if (saldoJenis.compareTo(angsuran.getJumlahAngsuran()) < 0) {
                throw new IllegalStateException(
                        "Saldo " + jenis.name() + " tidak cukup. Saldo: Rp " + formatRupiah(saldoJenis) +
                                ", dibutuhkan: Rp " + formatRupiah(angsuran.getJumlahAngsuran()));
            }
        }

        // Simpan jenis simpanan di keterangan untuk referensi saat eksekusi
        String ket = req.getKeterangan();
        if (metode == AngsuranKelompok.MetodeBayar.SIMPANAN && req.getJenisSimpanan() != null) {
            ket = "Bayar angsuran dari simpanan " + req.getJenisSimpanan();
        }

        TransaksiPending pending = TransaksiPending.builder()
                .user(user)
                .jenisTransaksi(TransaksiPending.JenisTransaksi.BAYAR_ANGSURAN_KELOMPOK)
                .angsuranKelompok(angsuran)
                .jumlah(angsuran.getJumlahAngsuran())
                .keterangan(ket)
                .buktiBayar(metode == AngsuranKelompok.MetodeBayar.TRANSFER ? req.getBuktiBayar() : null)
                .metodeBayar(metode)
                .status(TransaksiPending.StatusPending.PENDING)
                .build();
        pending = pendingRepo.save(pending);

        // Notif ke admin
        if (adminChatId != null && !adminChatId.isBlank()) {
            String kelompokNama = pinjaman.getKelompok().getNamaKelompok();
            String metodeTeks = metode == AngsuranKelompok.MetodeBayar.SIMPANAN
                    ? "Dari simpanan " + (req.getJenisSimpanan() != null ? req.getJenisSimpanan() : "")
                    : "Transfer";
            String pesan = "Ada pengajuan bayar angsuran kelompok!" + "\n\n"
                    + "Kelompok  : " + kelompokNama + "\n"
                    + "Anggota   : " + user.getNamaLengkap() + "\n"
                    + "Angsuran  : Ke-" + angsuran.getPeriodeKe() + "\n"
                    + "Jumlah    : Rp " + formatRupiah(angsuran.getJumlahAngsuran()) + "\n"
                    + "Metode    : " + metodeTeks + "\n\n"
                    + "Silakan buka halaman Pengajuan untuk memprosesnya.";
            telegramService.send(adminChatId, pesan);
        }

        return pending;
    }

    @Transactional
    public void eksekusiBayarAngsuran(TransaksiPending pending) {
        AngsuranKelompok angsuran = pending.getAngsuranKelompok();
        if (angsuran == null) throw new IllegalStateException("Angsuran kelompok tidak ditemukan");

        angsuran.setStatus(AngsuranKelompok.StatusAngsuran.SUDAH_BAYAR);
        angsuran.setTanggalBayar(LocalDate.now());
        angsuran.setDibayarOleh(pending.getUser());
        angsuran.setMetodeBayar(pending.getMetodeBayar() != null
                ? pending.getMetodeBayar() : AngsuranKelompok.MetodeBayar.TRANSFER);
        angsuran.setNoReferensiBayar("PAY-" + System.currentTimeMillis());
        angsuranRepo.save(angsuran);

        PinjamanKelompok pinjaman = angsuran.getPinjamanKelompok();
        pinjaman.setTotalSudahDibayar(
                pinjaman.getTotalSudahDibayar().add(angsuran.getJumlahAngsuran()));
        pinjaman.setSisaPinjaman(
                pinjaman.getSisaPinjaman().subtract(angsuran.getPokok()).max(BigDecimal.ZERO));

        // Jika SIMPANAN — potong saldo jenis yang dipilih
        if (angsuran.getMetodeBayar() == AngsuranKelompok.MetodeBayar.SIMPANAN) {
            // Coba parse jenis dari keterangan pending
            Simpanan.JenisSimpanan jenisPotong = Simpanan.JenisSimpanan.SUKARELA;
            if (pending.getKeterangan() != null) {
                if (pending.getKeterangan().contains("WAJIB")) jenisPotong = Simpanan.JenisSimpanan.WAJIB;
                else if (pending.getKeterangan().contains("POKOK")) jenisPotong = Simpanan.JenisSimpanan.POKOK;
            }
            potongSaldoSimpananJenis(pending.getUser(), angsuran.getJumlahAngsuran(), jenisPotong);
        }

        // Cek apakah semua angsuran lunas
        long belumBayar = pinjaman.getAngsuranList().stream()
                .filter(a -> a.getStatus() != AngsuranKelompok.StatusAngsuran.SUDAH_BAYAR)
                .count();
        if (belumBayar == 0) {
            pinjaman.setStatus(PinjamanKelompok.StatusPinjaman.LUNAS);
            pinjaman.setSisaPinjaman(BigDecimal.ZERO);
            // Terminate kelompok
            Kelompok kelompok = pinjaman.getKelompok();
            kelompok.setStatus(Kelompok.StatusKelompok.TERMINATED);
            kelompokRepo.save(kelompok);
            log.info("Kelompok {} TERMINATED setelah pinjaman lunas", kelompok.getKodeKelompok());

            // Notif ke semua anggota
            notifKeAnggotaKelompok(kelompok,
                    "🎉 *Pinjaman Kelompok LUNAS\\!*\n\n" +
                            "Selamat\\! Pinjaman kelompok *" + escMd(kelompok.getNamaKelompok()) + "* telah *LUNAS*\\.\n" +
                            "Kelompok ini sudah dibubarkan\\. Terima kasih atas kerja sama yang baik\\! 🙏");
        }
        pinjamanRepo.save(pinjaman);
    }

    // ─────────────────────────────────────────────────────────
    // TEGURAN
    // ─────────────────────────────────────────────────────────

    @Transactional
    public KelompokDto.TeguranResponse kirimTeguran(Long adminId, Long kelompokId,
                                                    KelompokDto.KirimTeguranRequest req) {
        User admin    = getUser(adminId);
        Kelompok kelompok = getKelompok(kelompokId);

        TeguranKelompok teguran = TeguranKelompok.builder()
                .kelompok(kelompok)
                .admin(admin)
                .pesan(req.getPesan())
                .build();
        teguran = teguranRepo.save(teguran);

        // Notif Telegram ke semua anggota kelompok
        notifKeAnggotaKelompok(kelompok,
                "⚠️ *Teguran dari Admin Koperasi*\n\n" +
                        "Kepada anggota kelompok *" + escMd(kelompok.getNamaKelompok()) + "*,\n\n" +
                        escMd(req.getPesan()) + "\n\n" +
                        "\\- Admin Koperasi Leyangan");

        log.info("Teguran dikirim ke kelompok {} oleh admin {}", kelompokId, adminId);
        return KelompokDto.TeguranResponse.builder()
                .id(teguran.getId())
                .namaAdmin(admin.getNamaLengkap())
                .pesan(teguran.getPesan())
                .sentAt(teguran.getSentAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<KelompokDto.TeguranResponse> getTeguranKelompok(Long kelompokId) {
        return teguranRepo.findByKelompokId(kelompokId).stream()
                .map(t -> KelompokDto.TeguranResponse.builder()
                        .id(t.getId())
                        .namaAdmin(t.getAdmin().getNamaLengkap())
                        .pesan(t.getPesan())
                        .sentAt(t.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    // PENCAIRAN DANA POOL PINJAMAN KELOMPOK
    // ─────────────────────────────────────────────────────────

    @Transactional
    public KelompokDto.PencairanResponse requestPencairan(Long userId, Long pinjamanId,
                                                          KelompokDto.RequestPencairanRequest req) {
        User user             = getUser(userId);
        PinjamanKelompok pinjaman = pinjamanRepo.findById(pinjamanId)
                .orElseThrow(() -> new IllegalArgumentException("Pinjaman tidak ditemukan"));

        if (pinjaman.getStatus() != PinjamanKelompok.StatusPinjaman.DISETUJUI) {
            throw new IllegalStateException("Pinjaman belum disetujui admin");
        }

        // Cek user adalah anggota kelompok
        Long kelompokId = pinjaman.getKelompok().getId();
        if (!anggotaRepo.existsByKelompokIdAndUserId(kelompokId, userId)) {
            throw new IllegalStateException("Anda bukan anggota kelompok ini");
        }

        // Hitung sisa pool
        BigDecimal totalCair = pencairanRepo.sumCairanDisetujui(pinjamanId);
        BigDecimal sisaPool  = pinjaman.getJumlahPinjaman().subtract(totalCair);

        if (req.getJumlah().compareTo(sisaPool) > 0) {
            throw new IllegalStateException(
                    "Jumlah melebihi sisa pool. Sisa tersedia: Rp " + formatRupiah(sisaPool));
        }

        // Hitung jatah rata per anggota
        long jumlahAnggota = anggotaRepo.countByKelompokId(kelompokId);
        BigDecimal jatahRata = pinjaman.getJumlahPinjaman()
                .divide(java.math.BigDecimal.valueOf(jumlahAnggota), 2, java.math.RoundingMode.HALF_UP);

        boolean melebihiJatah = req.getJumlah().compareTo(jatahRata) > 0;

        PencairanKelompok pencairan = PencairanKelompok.builder()
                .pinjamanKelompok(pinjaman)
                .user(user)
                .jumlah(req.getJumlah())
                .jatahRata(jatahRata)
                .melebihiJatah(melebihiJatah)
                .wajibBayarAngsuran(melebihiJatah)
                .catatan(req.getCatatan())
                .status(PencairanKelompok.StatusPencairan.PENDING)
                .build();
        pencairan = pencairanRepo.save(pencairan);

        // Notif ke leader
        User leader = pinjaman.getKelompok().getLeader();
        if (leader.getTelegramChatId() != null) {
            String pesanPencairan = "💰 *Request Pencairan Dana*\n\n"
                    + "Anggota *" + escMd(user.getNamaLengkap()) + "* mengajukan pencairan dana\\.\n"
                    + "💵 Jumlah: Rp " + escMd(formatRupiah(req.getJumlah())) + "\n"
                    + (melebihiJatah ? "⚠️ Melebihi jatah rata Rp " + escMd(formatRupiah(jatahRata)) + "\\. Wajib bayar min 1 angsuran\\.\n" : "")
                    + "\nSilakan setujui atau tolak di halaman Kelompok\\.";
            telegramService.send(leader.getTelegramChatId(), pesanPencairan);
        }

        log.info("Request pencairan Rp {} oleh user {} untuk pinjaman {}", req.getJumlah(), userId, pinjamanId);
        return mapToPencairanResponse(pencairan);
    }

    @Transactional
    public KelompokDto.PencairanResponse prosesPencairan(Long leaderId, Long pencairanId,
                                                         KelompokDto.ProsesPencairanRequest req) {
        PencairanKelompok pencairan = pencairanRepo.findById(pencairanId)
                .orElseThrow(() -> new IllegalArgumentException("Request pencairan tidak ditemukan"));

        // Validasi leader
        User leader = getUser(leaderId);
        Long kelompokId = pencairan.getPinjamanKelompok().getKelompok().getId();
        Kelompok kelompok = getKelompok(kelompokId);

        if (!kelompok.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("Hanya leader kelompok yang bisa menyetujui pencairan");
        }

        if (pencairan.getStatus() != PencairanKelompok.StatusPencairan.PENDING) {
            throw new IllegalStateException("Request pencairan sudah diproses sebelumnya");
        }

        if (req.getDisetujui()) {
            // Cek ulang sisa pool saat approval
            BigDecimal totalCair = pencairanRepo.sumCairanDisetujui(pencairan.getPinjamanKelompok().getId());
            BigDecimal sisaPool  = pencairan.getPinjamanKelompok().getJumlahPinjaman().subtract(totalCair);

            if (pencairan.getJumlah().compareTo(sisaPool) > 0) {
                throw new IllegalStateException(
                        "Sisa pool tidak cukup. Sisa tersedia: Rp " + formatRupiah(sisaPool));
            }

            pencairan.setStatus(PencairanKelompok.StatusPencairan.DISETUJUI);
            pencairan.setDisetujuiOleh(leader);
            pencairan.setTanggalApproval(java.time.LocalDateTime.now());
            pencairan.setCatatan(req.getCatatan());

            // Notif ke anggota
            if (pencairan.getUser().getTelegramChatId() != null) {
                String pesanSetujui = "✅ *Pencairan Dana Disetujui*\n\n"
                        + "Pencairan dana Rp *" + escMd(formatRupiah(pencairan.getJumlah())) + "* disetujui leader\\.\n"
                        + (pencairan.getWajibBayarAngsuran() ?
                        "⚠ Karena melebihi jatah, Anda wajib membayar minimal 1x angsuran\\." : "");
                telegramService.send(pencairan.getUser().getTelegramChatId(), pesanSetujui);
            }
        } else {
            pencairan.setStatus(PencairanKelompok.StatusPencairan.DITOLAK);
            pencairan.setCatatan(req.getCatatan());

            if (pencairan.getUser().getTelegramChatId() != null) {
                String pesanTolak = "❌ *Pencairan Dana Ditolak*\n\n"
                        + "Pencairan dana Rp " + escMd(formatRupiah(pencairan.getJumlah())) + " *ditolak* leader\\.\n"
                        + "Catatan: " + escMd(req.getCatatan() != null ? req.getCatatan() : "-");
                telegramService.send(pencairan.getUser().getTelegramChatId(), pesanTolak);
            }
        }

        pencairan = pencairanRepo.save(pencairan);
        log.info("Pencairan {} {} oleh leader {}", pencairanId, req.getDisetujui() ? "DISETUJUI" : "DITOLAK", leaderId);
        return mapToPencairanResponse(pencairan);
    }

    /**
     * Ambil semua member yang TIDAK terikat kelompok aktif manapun.
     * Member yang kelompoknya sudah TERMINATED tetap muncul di sini.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "member-bebas")
    public List<com.koperasi.dto.AuthDto.UserInfo> getMemberBebasKelompok() {
        // Single query - langsung filter di database
        return userRepo.findMemberBebasKelompokAktif().stream()
                .map(u -> com.koperasi.dto.AuthDto.UserInfo.builder()
                        .id(u.getId())
                        .namaLengkap(u.getNamaLengkap())
                        .nomorAnggota(u.getNomorAnggota())
                        .email(u.getEmail())
                        .telegramChatId(u.getTelegramChatId())
                        .totalSimpanan(u.getTotalSimpanan())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KelompokDto.PinjamanKelompokResponse getPinjamanDetail(Long pinjamanId) {
        PinjamanKelompok p = pinjamanRepo.findById(pinjamanId)
                .orElseThrow(() -> new IllegalArgumentException("Pinjaman tidak ditemukan"));
        return mapToPinjamanResponse(p, true);
    }

    @Transactional(readOnly = true)
    public List<KelompokDto.PencairanResponse> getRiwayatPencairan(Long pinjamanId) {
        return pencairanRepo.findByPinjamanKelompokId(pinjamanId)
                .stream().map(this::mapToPencairanResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KelompokDto.PencairanResponse> getPencairanPending(Long kelompokId, Long leaderId) {
        Kelompok kelompok = getKelompok(kelompokId);
        if (!kelompok.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("Hanya leader yang bisa melihat request pencairan");
        }
        return pencairanRepo.findPendingByKelompokId(kelompokId)
                .stream().map(this::mapToPencairanResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    // SCHEDULED: cek angsuran terlambat (dipanggil dari scheduler)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void cekAngsuranTerlambat() {
        List<AngsuranKelompok> terlambat = angsuranRepo.findTerlambat(LocalDate.now());
        for (AngsuranKelompok a : terlambat) {
            if (a.getStatus() == AngsuranKelompok.StatusAngsuran.BELUM_BAYAR) {
                a.setStatus(AngsuranKelompok.StatusAngsuran.TERLAMBAT);
                angsuranRepo.save(a);

                PinjamanKelompok p = a.getPinjamanKelompok();
                Kelompok kelompok  = p.getKelompok();

                // Notif ke admin
                if (adminChatId != null && !adminChatId.isBlank()) {
                    telegramService.send(adminChatId,
                            "🚨 *Angsuran Kelompok Terlambat\\!*\n\n" +
                                    "👥 Kelompok: *" + escMd(kelompok.getNamaKelompok()) + "*\n" +
                                    "📄 No\\. Pinjaman: `" + p.getNoPinjaman() + "`\n" +
                                    "📅 Angsuran ke\\-" + a.getPeriodeKe() + "\n" +
                                    "⏰ Jatuh tempo: " + a.getTanggalJatuhTempo() + "\n" +
                                    "💰 Jumlah: Rp " + escMd(formatRupiah(a.getJumlahAngsuran())) + "\n\n" +
                                    "Pertimbangkan untuk mengirim teguran ke kelompok ini\\.");
                }

                log.warn("Angsuran ke-{} kelompok {} TERLAMBAT", a.getPeriodeKe(), kelompok.getKodeKelompok());
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helper methods
    // ─────────────────────────────────────────────────────────

    private void notifKeAnggotaKelompok(Kelompok kelompok, String pesan) {
        List<KelompokAnggota> anggotaList = anggotaRepo.findByKelompokId(kelompok.getId());
        for (KelompokAnggota ka : anggotaList) {
            String chatId = ka.getUser().getTelegramChatId();
            if (chatId != null && !chatId.isBlank()) {
                telegramService.send(chatId, pesan);
            }
        }
    }

    private void potongSaldoSimpananJenis(User user, BigDecimal jumlah, Simpanan.JenisSimpanan jenis) {
        Simpanan s = Simpanan.builder()
                .user(user)
                .jenis(jenis)
                .jumlah(jumlah)
                .tipe(Simpanan.TipeTransaksi.TARIK)
                .keterangan("Bayar angsuran kelompok dari simpanan " + jenis.name())
                .tanggalTransaksi(java.time.LocalDateTime.now())
                .build();
        simpananRepo.save(s);
        user.setTotalSimpanan(simpananRepo.getTotalSimpananByUser(user.getId()));
        userRepo.save(user);
    }

    private void potongSaldoSimpanan(User user, BigDecimal jumlah) {
        // Potong dari simpanan sukarela dulu, lalu wajib
        BigDecimal saldoSukarela = simpananRepo.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.SUKARELA);
        if (saldoSukarela.compareTo(jumlah) >= 0) {
            Simpanan s = Simpanan.builder()
                    .user(user).jenis(Simpanan.JenisSimpanan.SUKARELA)
                    .jumlah(jumlah).tipe(Simpanan.TipeTransaksi.TARIK)
                    .keterangan("Bayar angsuran kelompok")
                    .tanggalTransaksi(java.time.LocalDateTime.now())
                    .build();
            simpananRepo.save(s);
        } else {
            // Potong dari sukarela habis, sisa dari wajib
            if (saldoSukarela.compareTo(BigDecimal.ZERO) > 0) {
                Simpanan s = Simpanan.builder()
                        .user(user).jenis(Simpanan.JenisSimpanan.SUKARELA)
                        .jumlah(saldoSukarela).tipe(Simpanan.TipeTransaksi.TARIK)
                        .keterangan("Bayar angsuran kelompok")
                        .tanggalTransaksi(java.time.LocalDateTime.now())
                        .build();
                simpananRepo.save(s);
            }
            BigDecimal sisaDariWajib = jumlah.subtract(saldoSukarela);
            Simpanan sw = Simpanan.builder()
                    .user(user).jenis(Simpanan.JenisSimpanan.WAJIB)
                    .jumlah(sisaDariWajib).tipe(Simpanan.TipeTransaksi.TARIK)
                    .keterangan("Bayar angsuran kelompok")
                    .tanggalTransaksi(java.time.LocalDateTime.now())
                    .build();
            simpananRepo.save(sw);
        }
        // Update total simpanan user
        user.setTotalSimpanan(simpananRepo.getTotalSimpananByUser(user.getId()));
        userRepo.save(user);
    }

    private List<AngsuranKelompok> generateJadwal(PinjamanKelompok pinjaman) {
        List<AngsuranKelompok> jadwal = new ArrayList<>();
        BigDecimal angsuran = pinjaman.getAngsuranPerBulan();
        BigDecimal sisaPokok = pinjaman.getJumlahPinjaman();
        BigDecimal bungaRate = pinjaman.getBungaPerBulan().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        for (int i = 1; i <= pinjaman.getTenorBulan(); i++) {
            BigDecimal bunga = sisaPokok.multiply(bungaRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pokok = (i < pinjaman.getTenorBulan())
                    ? angsuran.subtract(bunga)
                    : sisaPokok;
            BigDecimal total = (i < pinjaman.getTenorBulan()) ? angsuran : pokok.add(bunga);

            jadwal.add(AngsuranKelompok.builder()
                    .pinjamanKelompok(pinjaman)
                    .periodeKe(i)
                    .jumlahAngsuran(total.setScale(2, RoundingMode.HALF_UP))
                    .pokok(pokok.setScale(2, RoundingMode.HALF_UP))
                    .bunga(bunga)
                    .tanggalJatuhTempo(pinjaman.getTanggalDisetujui().plusMonths(i))
                    .status(AngsuranKelompok.StatusAngsuran.BELUM_BAYAR)
                    .build());
            sisaPokok = sisaPokok.subtract(pokok).max(BigDecimal.ZERO);
        }
        return jadwal;
    }

    private BigDecimal hitungAngsuran(BigDecimal pokok, BigDecimal bungaPersen, int tenor) {
        BigDecimal r   = bungaPersen.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(tenor);
        return pokok.multiply(r).multiply(pow)
                .divide(pow.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private String generateKodeKelompok() {
        long count = kelompokRepo.count() + 1;
        return String.format("KLP-%04d", count);
    }

    private String generateNoPinjaman() {
        return "PKL-" + System.currentTimeMillis();
    }

    private String formatRupiah(BigDecimal v) {
        if (v == null) return "0";
        return NumberFormat.getNumberInstance(new Locale("id","ID")).format(v);
    }

    private String escMd(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }

    private User getUser(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
    }

    private Kelompok getKelompok(Long id) {
        return kelompokRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Kelompok tidak ditemukan"));
    }

    // ─────────────────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────────────────

    private KelompokDto.KelompokResponse mapToResponse(Kelompok k, boolean withAnggota) {
        List<KelompokAnggota> anggotaList = withAnggota ? anggotaRepo.findByKelompokId(k.getId()) : List.of();

        PinjamanKelompok pinjamanAktif = null;
        try {
            pinjamanAktif = pinjamanRepo.findActivePinjamanByKelompokId(k.getId()).orElse(null);
        } catch (Exception ignored) {}

        return KelompokDto.KelompokResponse.builder()
                .id(k.getId())
                .kodeKelompok(k.getKodeKelompok())
                .namaKelompok(k.getNamaKelompok())
                .deskripsi(k.getDeskripsi())
                .leaderId(k.getLeader().getId())
                .namaLeader(k.getLeader().getNamaLengkap())
                .status(k.getStatus().name())
                .jumlahAnggota((int) anggotaRepo.countByKelompokId(k.getId()))
                .anggotaList(withAnggota ? anggotaList.stream().map(ka ->
                        KelompokDto.AnggotaInfo.builder()
                                .id(ka.getUser().getId())
                                .namaLengkap(ka.getUser().getNamaLengkap())
                                .nomorAnggota(ka.getUser().getNomorAnggota())
                                .isLeader(ka.getUser().getId().equals(k.getLeader().getId()))
                                .joinedAt(ka.getJoinedAt())
                                .build()
                ).collect(Collectors.toList()) : null)
                .pinjamanAktif(pinjamanAktif != null ? mapToPinjamanResponse(pinjamanAktif, true) : null)
                .createdAt(k.getCreatedAt())
                .build();
    }

    private KelompokDto.PencairanResponse mapToPencairanResponse(PencairanKelompok p) {
        return KelompokDto.PencairanResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .namaAnggota(p.getUser().getNamaLengkap())
                .jumlah(p.getJumlah())
                .jatahRata(p.getJatahRata())
                .melebihiJatah(p.getMelebihiJatah())
                .wajibBayarAngsuran(p.getWajibBayarAngsuran())
                .status(p.getStatus().name())
                .namaLeaderApproval(p.getDisetujuiOleh() != null ? p.getDisetujuiOleh().getNamaLengkap() : null)
                .catatan(p.getCatatan())
                .tanggalApproval(p.getTanggalApproval())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public KelompokDto.PinjamanKelompokResponse mapToPinjamanResponse(PinjamanKelompok p, boolean withJadwal) {
        List<KelompokDto.AngsuranKelompokResponse> jadwal = withJadwal
                ? angsuranRepo.findByPinjamanKelompokId(p.getId()).stream()
                .map(a -> KelompokDto.AngsuranKelompokResponse.builder()
                        .id(a.getId())
                        .periodeKe(a.getPeriodeKe())
                        .jumlahAngsuran(a.getJumlahAngsuran())
                        .pokok(a.getPokok())
                        .bunga(a.getBunga())
                        .tanggalJatuhTempo(a.getTanggalJatuhTempo())
                        .tanggalBayar(a.getTanggalBayar())
                        .status(a.getStatus().name())
                        .dibayarOleh(a.getDibayarOleh() != null ? a.getDibayarOleh().getNamaLengkap() : null)
                        .metodeBayar(a.getMetodeBayar() != null ? a.getMetodeBayar().name() : null)
                        .terlambat(a.getStatus() == AngsuranKelompok.StatusAngsuran.TERLAMBAT)
                        .adaPendingBayar(angsuranRepo.countPendingByAngsuranKelompokId(a.getId()) > 0)
                        .build())
                .collect(Collectors.toList())
                : null;

        return KelompokDto.PinjamanKelompokResponse.builder()
                .id(p.getId())
                .noPinjaman(p.getNoPinjaman())
                .kelompokId(p.getKelompok().getId())
                .namaKelompok(p.getKelompok().getNamaKelompok())
                .pengajuId(p.getPengaju().getId())
                .namaPengaju(p.getPengaju().getNamaLengkap())
                .jumlahPinjaman(p.getJumlahPinjaman())
                .bungaPerBulan(p.getBungaPerBulan())
                .tenorBulan(p.getTenorBulan())
                .angsuranPerBulan(p.getAngsuranPerBulan())
                .totalSudahDibayar(p.getTotalSudahDibayar())
                .sisaPinjaman(p.getSisaPinjaman())
                .tujuanPinjaman(p.getTujuanPinjaman())
                .status(p.getStatus().name())
                .tanggalPengajuan(p.getTanggalPengajuan())
                .tanggalDisetujui(p.getTanggalDisetujui())
                .tanggalJatuhTempo(p.getTanggalJatuhTempo())
                .keteranganAdmin(p.getKeteranganAdmin())
                .jadwalAngsuran(jadwal)
                .riwayatPencairan(withJadwal ? pencairanRepo.findByPinjamanKelompokId(p.getId())
                        .stream().map(this::mapToPencairanResponse).collect(java.util.stream.Collectors.toList()) : null)
                .totalTercairkan(pencairanRepo.sumCairanDisetujui(p.getId()))
                .sisaPool(p.getJumlahPinjaman().subtract(pencairanRepo.sumCairanDisetujui(p.getId())))
                .jatahRataPerAnggota(anggotaRepo.countByKelompokId(p.getKelompok().getId()) > 0
                        ? p.getJumlahPinjaman().divide(
                        java.math.BigDecimal.valueOf(anggotaRepo.countByKelompokId(p.getKelompok().getId())),
                        2, java.math.RoundingMode.HALF_UP)
                        : java.math.BigDecimal.ZERO)
                .jumlahAnggota((int) anggotaRepo.countByKelompokId(p.getKelompok().getId()))
                .pencairanPending(pencairanRepo.countByPinjamanKelompokIdAndStatus(
                        p.getId(), PencairanKelompok.StatusPencairan.PENDING))
                .build();
    }
}