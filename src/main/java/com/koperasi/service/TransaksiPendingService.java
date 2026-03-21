package com.koperasi.service;

import com.koperasi.dto.TransaksiPendingDto;
import com.koperasi.entity.*;
import com.koperasi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransaksiPendingService {

    private final TransaksiPendingRepository  pendingRepo;
    private final TelegramService             telegramService;
    private final KelompokService             kelompokService;
    private final SimpananRepository          simpananRepository;
    private final UserRepository              userRepository;
    private final AngsuranPinjamanRepository  angsuranRepository;
    private final PinjamanRepository          pinjamanRepository;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    // ─────────────────────────────────────────────────────────
    // MEMBER: Ajukan transaksi
    // ─────────────────────────────────────────────────────────

    @Transactional
    public TransaksiPendingDto.TransaksiPendingResponse ajukanSetor(
            Long userId, TransaksiPendingDto.AjukanSetorRequest request) {

        User user = getUser(userId);

        TransaksiPending pending = TransaksiPending.builder()
                .user(user)
                .jenisTransaksi(TransaksiPending.JenisTransaksi.SETOR_SIMPANAN)
                .jenisSimpanan(request.getJenisSimpanan())
                .jumlah(request.getJumlah())
                .keterangan(request.getKeterangan())
                .buktiBayar(request.getBuktiBayar())
                .status(TransaksiPending.StatusPending.PENDING)
                .build();

        pending = pendingRepo.save(pending);
        log.info("Pengajuan setor {} {} oleh user {}", request.getJenisSimpanan(), request.getJumlah(), userId);
        kirimNotifPengajuanKeAdmin(user.getNamaLengkap(), "Setor Simpanan",
                request.getJenisSimpanan().name(), request.getJumlah());
        return mapToResponse(pending);
    }

    @Transactional
    public TransaksiPendingDto.TransaksiPendingResponse ajukanTarik(
            Long userId, TransaksiPendingDto.AjukanTarikRequest request) {

        User user = getUser(userId);

        if (request.getJenisSimpanan() == Simpanan.JenisSimpanan.POKOK) {
            throw new IllegalArgumentException("Simpanan Pokok tidak dapat ditarik selama masih menjadi anggota");
        }

        BigDecimal saldo = simpananRepository.getSaldoByUserAndJenis(userId, request.getJenisSimpanan());
        if (saldo.compareTo(request.getJumlah()) < 0) {
            throw new IllegalArgumentException(
                    "Saldo " + request.getJenisSimpanan() + " tidak cukup. Saldo saat ini: Rp " + saldo);
        }

        TransaksiPending pending = TransaksiPending.builder()
                .user(user)
                .jenisTransaksi(TransaksiPending.JenisTransaksi.TARIK_SIMPANAN)
                .jenisSimpanan(request.getJenisSimpanan())
                .jumlah(request.getJumlah())
                .keterangan(request.getKeterangan())
                .buktiBayar(request.getBuktiBayar())
                .status(TransaksiPending.StatusPending.PENDING)
                .build();

        pending = pendingRepo.save(pending);
        log.info("Pengajuan tarik {} {} oleh user {}", request.getJenisSimpanan(), request.getJumlah(), userId);
        kirimNotifPengajuanKeAdmin(user.getNamaLengkap(), "Tarik Simpanan",
                request.getJenisSimpanan().name(), request.getJumlah());
        return mapToResponse(pending);
    }

    @Transactional
    public TransaksiPendingDto.TransaksiPendingResponse ajukanBayarAngsuran(
            Long userId, TransaksiPendingDto.AjukanBayarAngsuranRequest request) {

        User user = getUser(userId);

        AngsuranPinjaman angsuran = angsuranRepository.findById(request.getAngsuranId())
                .orElseThrow(() -> new IllegalArgumentException("Angsuran tidak ditemukan"));

        if (!angsuran.getPinjaman().getUser().getId().equals(userId)) {
            throw new IllegalStateException("Angsuran ini bukan milik Anda");
        }

        if (angsuran.getStatus() == AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR) {
            throw new IllegalStateException("Angsuran ini sudah dibayar");
        }

        long existingPending = pendingRepo.countPendingByAngsuranId(request.getAngsuranId());
        if (existingPending > 0) {
            throw new IllegalStateException("Pengajuan pembayaran angsuran ini sudah ada dan sedang menunggu persetujuan admin");
        }

        TransaksiPending pending = TransaksiPending.builder()
                .user(user)
                .jenisTransaksi(TransaksiPending.JenisTransaksi.BAYAR_ANGSURAN)
                .angsuran(angsuran)
                .jumlah(angsuran.getJumlahAngsuran())
                .keterangan(request.getKeterangan())
                .buktiBayar(request.getBuktiBayar())
                .status(TransaksiPending.StatusPending.PENDING)
                .build();

        pending = pendingRepo.save(pending);
        log.info("Pengajuan bayar angsuran ke-{} oleh user {}", angsuran.getPeriodeKe(), userId);
        return mapToResponse(pending);
    }

    // ─────────────────────────────────────────────────────────
    // MEMBER: Riwayat
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransaksiPendingDto.TransaksiPendingResponse> getRiwayatPengajuan(Long userId, Pageable pageable) {
        return pendingRepo.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN: Lihat & proses
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TransaksiPendingDto.TransaksiPendingResponse> getAllPending() {
        return pendingRepo.findAllPending().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransaksiPendingDto.TransaksiPendingResponse> getAllTransaksi(String status, Pageable pageable) {
        Page<TransaksiPending> page;
        if (status != null && !status.isEmpty()) {
            try {
                TransaksiPending.StatusPending statusEnum = TransaksiPending.StatusPending.valueOf(status.toUpperCase());
                page = pendingRepo.findByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                page = pendingRepo.findAllWithUser(pageable);
            }
        } else {
            page = pendingRepo.findAllWithUser(pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransaksiPendingDto.PendingSummary getPendingSummary() {
        return TransaksiPendingDto.PendingSummary.builder()
                .totalPending(pendingRepo.countPending())
                .build();
    }

    @Transactional
    public TransaksiPendingDto.TransaksiPendingResponse prosesApproval(
            Long pendingId, Long adminId, TransaksiPendingDto.ApprovalRequest request) {

        TransaksiPending pending = pendingRepo.findById(pendingId)
                .orElseThrow(() -> new IllegalArgumentException("Pengajuan tidak ditemukan"));

        if (pending.getStatus() != TransaksiPending.StatusPending.PENDING) {
            throw new IllegalStateException("Pengajuan ini sudah diproses sebelumnya");
        }

        User admin = getUser(adminId);
        pending.setApprovedBy(admin);
        pending.setTanggalApproval(LocalDateTime.now());
        pending.setCatatanAdmin(request.getCatatanAdmin());

        if (request.getDisetujui()) {
            // Validasi saldo simpanan sebelum approve BAYAR_ANGSURAN_KELOMPOK via SIMPANAN
            if (pending.getJenisTransaksi() == TransaksiPending.JenisTransaksi.BAYAR_ANGSURAN_KELOMPOK
                    && pending.getMetodeBayar() == com.koperasi.entity.AngsuranKelompok.MetodeBayar.SIMPANAN) {
                validasiSaldoSimpananCukup(pending);
            }

            switch (pending.getJenisTransaksi()) {
                case SETOR_SIMPANAN         -> eksekusiSetor(pending);
                case TARIK_SIMPANAN         -> eksekusiTarik(pending);
                case BAYAR_ANGSURAN         -> eksekusiBayarAngsuran(pending);
                case BAYAR_ANGSURAN_KELOMPOK -> kelompokService.eksekusiBayarAngsuran(pending);
            }
            pending.setStatus(TransaksiPending.StatusPending.DISETUJUI);
            log.info("Admin {} MENYETUJUI pengajuan #{} ({})", adminId, pendingId, pending.getJenisTransaksi());
            // Notif ke member
            String memberChatId = pending.getUser().getTelegramChatId();
            if (memberChatId != null && !memberChatId.isBlank()) {
                telegramService.notifTransaksiDisetujui(
                        memberChatId,
                        pending.getUser().getNamaLengkap(),
                        jenisLabel(pending.getJenisTransaksi()),
                        formatRupiah(pending.getJumlah())
                );
            }
        } else {
            pending.setStatus(TransaksiPending.StatusPending.DITOLAK);
            log.info("Admin {} MENOLAK pengajuan #{} ({})", adminId, pendingId, pending.getJenisTransaksi());
            // Notif ke member
            String memberChatIdTolak = pending.getUser().getTelegramChatId();
            if (memberChatIdTolak != null && !memberChatIdTolak.isBlank()) {
                telegramService.notifTransaksiDitolak(
                        memberChatIdTolak,
                        pending.getUser().getNamaLengkap(),
                        jenisLabel(pending.getJenisTransaksi()),
                        formatRupiah(pending.getJumlah()),
                        request.getCatatanAdmin()
                );
            }
        }

        pending = pendingRepo.save(pending);
        return mapToResponse(pending);
    }

    // ─────────────────────────────────────────────────────────
    // Private: eksekusi
    // ─────────────────────────────────────────────────────────

    private void eksekusiSetor(TransaksiPending pending) {
        Long userId = pending.getUser().getId();
        User user   = getUser(userId);

        Simpanan simpanan = Simpanan.builder()
                .user(user)
                .jenis(pending.getJenisSimpanan())
                .jumlah(pending.getJumlah())
                .tipe(Simpanan.TipeTransaksi.SETOR)
                .keterangan(pending.getKeterangan())
                .tanggalTransaksi(LocalDateTime.now())
                .build();

        simpananRepository.save(simpanan);

        BigDecimal totalBaru = simpananRepository.getTotalSimpananByUser(userId);
        user.setTotalSimpanan(totalBaru);
        userRepository.save(user);
    }

    private void eksekusiTarik(TransaksiPending pending) {
        Long userId = pending.getUser().getId();
        User user   = getUser(userId);

        BigDecimal saldo = simpananRepository.getSaldoByUserAndJenis(userId, pending.getJenisSimpanan());
        if (saldo.compareTo(pending.getJumlah()) < 0) {
            throw new IllegalStateException(
                    "Saldo tidak cukup saat approval. Saldo " + pending.getJenisSimpanan() + ": Rp " + saldo);
        }

        Simpanan simpanan = Simpanan.builder()
                .user(user)
                .jenis(pending.getJenisSimpanan())
                .jumlah(pending.getJumlah())
                .tipe(Simpanan.TipeTransaksi.TARIK)
                .keterangan(pending.getKeterangan())
                .tanggalTransaksi(LocalDateTime.now())
                .build();

        simpananRepository.save(simpanan);

        BigDecimal totalBaru = simpananRepository.getTotalSimpananByUser(userId);
        user.setTotalSimpanan(totalBaru);
        userRepository.save(user);
    }

    private void eksekusiBayarAngsuran(TransaksiPending pending) {
        AngsuranPinjaman angsuran = angsuranRepository.findById(pending.getAngsuran().getId())
                .orElseThrow(() -> new IllegalArgumentException("Angsuran tidak ditemukan"));

        if (angsuran.getStatus() == AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR) {
            throw new IllegalStateException("Angsuran ini sudah dibayar");
        }

        angsuran.setStatus(AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR);
        angsuran.setTanggalBayar(LocalDate.now());
        angsuran.setNoReferensiBayar("PAY-" + System.currentTimeMillis());
        angsuranRepository.save(angsuran);

        Pinjaman pinjaman = angsuran.getPinjaman();
        pinjaman.setTotalSudahDibayar(pinjaman.getTotalSudahDibayar().add(angsuran.getJumlahAngsuran()));
        pinjaman.setSisaPinjaman(pinjaman.getSisaPinjaman().subtract(angsuran.getPokok()).max(BigDecimal.ZERO));

        long belumBayar = pinjaman.getAngsuranList().stream()
                .filter(a -> a.getStatus() != AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR)
                .count();
        // Tandai LUNAS hanya jika SEMUA angsuran sudah dibayar (count == 0)
        if (belumBayar == 0) {
            pinjaman.setStatus(Pinjaman.StatusPinjaman.LUNAS);
            pinjaman.setSisaPinjaman(BigDecimal.ZERO);
        }

        pinjamanRepository.save(pinjaman);
    }

    private void kirimNotifPengajuanKeAdmin(String namaMember, String jenis, String detail, java.math.BigDecimal jumlah) {
        if (adminChatId == null || adminChatId.isBlank()) return;
        String detailStr = (detail != null && !detail.isBlank()) ? " (" + detail + ")" : "";
        String pesan = "Ada pengajuan baru masuk!" + "\n\n"
                + "Jenis    : " + jenis + detailStr + "\n"
                + "Anggota  : " + namaMember + "\n"
                + "Jumlah   : Rp " + formatRupiah(jumlah) + "\n\n"
                + "Silakan buka halaman Pengajuan untuk memprosesnya.";
        telegramService.send(adminChatId, pesan);
    }

    private void validasiSaldoSimpananCukup(TransaksiPending pending) {
        // Parse jenis simpanan dari keterangan
        Simpanan.JenisSimpanan jenis = Simpanan.JenisSimpanan.SUKARELA;
        String ket = pending.getKeterangan();
        if (ket != null) {
            if (ket.contains("WAJIB"))   jenis = Simpanan.JenisSimpanan.WAJIB;
            else if (ket.contains("POKOK")) jenis = Simpanan.JenisSimpanan.POKOK;
        }

        java.math.BigDecimal saldo = simpananRepository.getSaldoByUserAndJenis(
                pending.getUser().getId(), jenis);
        java.math.BigDecimal dibutuhkan = pending.getJumlah();

        if (saldo.compareTo(dibutuhkan) < 0) {
            throw new IllegalStateException(
                    "Saldo simpanan " + jenis.name() + " anggota " +
                            pending.getUser().getNamaLengkap() + " tidak mencukupi. " +
                            "Saldo: Rp " + formatRupiah(saldo) + ", " +
                            "dibutuhkan: Rp " + formatRupiah(dibutuhkan) + ". " +
                            "Tolak pengajuan ini atau minta member mengubah metode pembayaran."
            );
        }
    }

    private String jenisLabel(TransaksiPending.JenisTransaksi jenis) {
        return switch (jenis) {
            case SETOR_SIMPANAN          -> "Setor Simpanan";
            case TARIK_SIMPANAN          -> "Tarik Simpanan";
            case BAYAR_ANGSURAN          -> "Bayar Angsuran";
            case BAYAR_ANGSURAN_KELOMPOK -> "Bayar Angsuran Kelompok";
        };
    }

    private String formatRupiah(BigDecimal amount) {
        if (amount == null) return "0";
        return NumberFormat.getNumberInstance(new Locale("id", "ID")).format(amount);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
    }

    private TransaksiPendingDto.TransaksiPendingResponse mapToResponse(TransaksiPending t) {
        String noPinjaman = null;
        Integer periodeKe = null;
        Long angsuranId   = null;

        if (t.getAngsuran() != null) {
            angsuranId = t.getAngsuran().getId();
            periodeKe  = t.getAngsuran().getPeriodeKe();
            try { noPinjaman = t.getAngsuran().getPinjaman().getNoPinjaman(); } catch (Exception ignored) {}
        }

        return TransaksiPendingDto.TransaksiPendingResponse.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .namaAnggota(t.getUser().getNamaLengkap())
                .nomorAnggota(t.getUser().getNomorAnggota())
                .jenisTransaksi(t.getJenisTransaksi().name())
                .jenisSimpanan(t.getJenisSimpanan() != null ? t.getJenisSimpanan().name() : null)
                .jumlah(t.getJumlah())
                .keterangan(t.getKeterangan())
                .angsuranId(angsuranId)
                .periodeAngsuran(periodeKe)
                .noPinjaman(noPinjaman)
                .status(t.getStatus().name())
                .catatanAdmin(t.getCatatanAdmin())
                .buktiBayar(t.getBuktiBayar())
                .approvedByName(t.getApprovedBy() != null ? t.getApprovedBy().getNamaLengkap() : null)
                .tanggalApproval(t.getTanggalApproval())
                .createdAt(t.getCreatedAt())
                .build();
    }
}