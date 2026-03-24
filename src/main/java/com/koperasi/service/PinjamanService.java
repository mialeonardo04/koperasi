package com.koperasi.service;

import com.koperasi.dto.PinjamanDto;
import com.koperasi.entity.AngsuranPinjaman;
import com.koperasi.entity.Pinjaman;
import com.koperasi.entity.User;
import com.koperasi.repository.AngsuranPinjamanRepository;
import com.koperasi.repository.PinjamanRepository;
import com.koperasi.repository.UserRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinjamanService {

    private final PinjamanRepository       pinjamanRepository;
    private final AngsuranPinjamanRepository angsuranRepository;
    private final UserRepository             userRepository;
    private final TelegramService            telegramService;
    private final AuditLogService           auditLogService;

    @Value("${app.telegram.admin-chat-id:}")
    private String adminChatId;

    private static final BigDecimal DEFAULT_BUNGA = new BigDecimal("1.5");

    @Transactional
    public PinjamanDto.PinjamanResponse ajukanPinjaman(Long userId, PinjamanDto.PengajuanRequest request) {
        User user = getUser(userId);

        boolean adaPinjamanAktif = pinjamanRepository.existsByUserIdAndStatusIn(
                userId, List.of(Pinjaman.StatusPinjaman.DISETUJUI, Pinjaman.StatusPinjaman.PENDING));
        if (adaPinjamanAktif) {
            throw new IllegalStateException("Anda masih memiliki pinjaman aktif atau dalam proses persetujuan");
        }

        BigDecimal angsuranPerBulan = hitungAngsuran(
                request.getJumlahPinjaman(), DEFAULT_BUNGA, request.getTenorBulan());

        Pinjaman pinjaman = Pinjaman.builder()
                .user(user)
                .jumlahPinjaman(request.getJumlahPinjaman())
                .bungaPerBulan(DEFAULT_BUNGA)
                .tenorBulan(request.getTenorBulan())
                .angsuranPerBulan(angsuranPerBulan)
                .sisaPinjaman(request.getJumlahPinjaman())
                .tujuanPinjaman(request.getTujuanPinjaman())
                .status(Pinjaman.StatusPinjaman.PENDING)
                .build();

        pinjaman = pinjamanRepository.save(pinjaman);
        log.info("Pengajuan pinjaman {} oleh user {}", pinjaman.getNoPinjaman(), userId);

        // Notif ke admin: ada pengajuan pinjaman baru
        if (adminChatId != null && !adminChatId.isBlank()) {
            telegramService.notifPinjamanBaru(
                    adminChatId,
                    user.getNamaLengkap(),
                    user.getNomorAnggota(),
                    formatRupiah(pinjaman.getJumlahPinjaman()),
                    pinjaman.getTujuanPinjaman() != null ? pinjaman.getTujuanPinjaman() : "-"
            );
        }

        return mapToResponse(pinjaman, user, false);
    }

    @Transactional
    public PinjamanDto.PinjamanResponse prosesPersetujuan(Long pinjamanId, User admin, PinjamanDto.ApprovalRequest request) {
        Pinjaman pinjaman = pinjamanRepository.findById(pinjamanId)
                .orElseThrow(() -> new IllegalArgumentException("Pinjaman tidak ditemukan"));

        if (pinjaman.getStatus() != Pinjaman.StatusPinjaman.PENDING) {
            throw new IllegalStateException("Pinjaman sudah diproses sebelumnya");
        }

        String statusSebelumnya = pinjaman.getStatus().name();
        // Ambil user secara eksplisit selagi sesi masih terbuka
        User user = getUser(pinjaman.getUser().getId());

        if (request.getDisetujui()) {
            BigDecimal bunga = request.getBungaPerBulan() != null ? request.getBungaPerBulan() : DEFAULT_BUNGA;
            BigDecimal angsuran = hitungAngsuran(pinjaman.getJumlahPinjaman(), bunga, pinjaman.getTenorBulan());

            pinjaman.setBungaPerBulan(bunga);
            pinjaman.setAngsuranPerBulan(angsuran);
            pinjaman.setStatus(Pinjaman.StatusPinjaman.DISETUJUI);
            pinjaman.setTanggalDisetujui(LocalDate.now());
            pinjaman.setTanggalJatuhTempo(LocalDate.now().plusMonths(pinjaman.getTenorBulan()));
            pinjaman.setKeteranganAdmin(request.getKeteranganAdmin());
            // Jangan replace referensi list (orphanRemoval = true akan error)
            // Gunakan clear() + addAll() agar Hibernate tetap track koleksi yang sama
            pinjaman.getAngsuranList().clear();
            pinjaman.getAngsuranList().addAll(generateJadwalAngsuran(pinjaman));

            log.info("Pinjaman {} DISETUJUI", pinjaman.getNoPinjaman());
            // Notif ke member
            if (user.getTelegramChatId() != null) {
                telegramService.notifPinjamanDisetujui(
                        user.getTelegramChatId(),
                        user.getNamaLengkap(),
                        pinjaman.getNoPinjaman(),
                        formatRupiah(pinjaman.getJumlahPinjaman()),
                        String.valueOf(pinjaman.getTenorBulan()),
                        formatRupiah(pinjaman.getAngsuranPerBulan())
                );
            }
        } else {
            pinjaman.setStatus(Pinjaman.StatusPinjaman.DITOLAK);
            pinjaman.setKeteranganAdmin(request.getKeteranganAdmin());
            log.info("Pinjaman {} DITOLAK", pinjaman.getNoPinjaman());
            // Notif ke member
            if (user.getTelegramChatId() != null) {
                telegramService.notifPinjamanDitolak(
                        user.getTelegramChatId(),
                        user.getNamaLengkap(),
                        formatRupiah(pinjaman.getJumlahPinjaman()),
                        request.getKeteranganAdmin()
                );
            }
        }

        pinjaman = pinjamanRepository.save(pinjaman);

        // Audit Log
        auditLogService.log(
                admin,
                request.getDisetujui() ? "APPROVE_LOAN" : "REJECT_LOAN",
                "pinjaman",
                pinjaman.getId().toString(),
                String.format("status: %s", statusSebelumnya),
                String.format("status: %s, admin_ket: %s", pinjaman.getStatus().name(), request.getKeteranganAdmin())
        );

        return mapToResponse(pinjaman, user, true);
    }

    @Transactional
    public PinjamanDto.PinjamanResponse bayarAngsuran(Long userId, PinjamanDto.BayarAngsuranRequest request) {
        AngsuranPinjaman angsuran = angsuranRepository.findById(request.getAngsuranId())
                .orElseThrow(() -> new IllegalArgumentException("Angsuran tidak ditemukan"));

        if (!angsuran.getPinjaman().getUser().getId().equals(userId)) {
            throw new IllegalStateException("Angsuran ini bukan milik Anda");
        }

        if (angsuran.getStatus() == AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR) {
            throw new IllegalStateException("Angsuran ini sudah dibayar");
        }

        angsuran.setStatus(AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR);
        angsuran.setTanggalBayar(LocalDate.now());
        angsuran.setNoReferensiBayar("PAY-" + System.currentTimeMillis());
        angsuranRepository.save(angsuran);

        Pinjaman pinjaman = angsuran.getPinjaman();
        User user = getUser(userId);

        BigDecimal totalBayar = pinjaman.getTotalSudahDibayar().add(angsuran.getJumlahAngsuran());
        pinjaman.setTotalSudahDibayar(totalBayar);

        BigDecimal sisaPinjaman = pinjaman.getSisaPinjaman().subtract(angsuran.getPokok());
        pinjaman.setSisaPinjaman(sisaPinjaman.max(BigDecimal.ZERO));

        long belumBayar = pinjaman.getAngsuranList().stream()
                .filter(a -> a.getStatus() != AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR)
                .count();
        // Tandai LUNAS hanya jika SEMUA angsuran sudah dibayar
        if (belumBayar == 0) {
            pinjaman.setStatus(Pinjaman.StatusPinjaman.LUNAS);
            pinjaman.setSisaPinjaman(BigDecimal.ZERO);
        }

        pinjaman = pinjamanRepository.save(pinjaman);
        log.info("Pembayaran angsuran ke-{} pinjaman {} oleh user {}",
                angsuran.getPeriodeKe(), pinjaman.getNoPinjaman(), userId);

        return mapToResponse(pinjaman, user, true);
    }

    @Transactional(readOnly = true)
    public Long countPending() {
        return pinjamanRepository.countPending();
    }

    public Page<PinjamanDto.PinjamanResponse> getRiwayatPinjaman(Long userId, Pageable pageable) {
        return pinjamanRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(p -> mapToResponse(p, p.getUser(), false));
    }

    @Transactional(readOnly = true)
    public PinjamanDto.PinjamanResponse getDetailPinjaman(Long pinjamanId, Long userId, boolean isAdmin) {
        Pinjaman pinjaman = pinjamanRepository.findById(pinjamanId)
                .orElseThrow(() -> new IllegalArgumentException("Pinjaman tidak ditemukan"));

        User user = getUser(pinjaman.getUser().getId());

        if (!isAdmin && !user.getId().equals(userId)) {
            throw new IllegalStateException("Anda tidak memiliki akses ke pinjaman ini");
        }

        return mapToResponse(pinjaman, user, true);
    }

    @Transactional(readOnly = true)
    public Page<PinjamanDto.PinjamanResponse> getAllPinjaman(Long userId, String status, Pageable pageable) {
        Pinjaman.StatusPinjaman statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = Pinjaman.StatusPinjaman.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return pinjamanRepository.findWithFilter(userId, statusEnum, pageable)
                .map(p -> mapToResponse(p, p.getUser(), false));
    }

    @Transactional(readOnly = true)
    public List<PinjamanDto.AngsuranResponse> getJadwalAngsuran(Long pinjamanId, Long userId, boolean isAdmin) {
        Pinjaman pinjaman = pinjamanRepository.findById(pinjamanId)
                .orElseThrow(() -> new IllegalArgumentException("Pinjaman tidak ditemukan"));

        User user = getUser(pinjaman.getUser().getId());

        if (!isAdmin && !user.getId().equals(userId)) {
            throw new IllegalStateException("Anda tidak memiliki akses ke pinjaman ini");
        }

        return angsuranRepository.findByPinjamanIdOrderByPeriodeKe(pinjamanId)
                .stream().map(this::mapAngsuranToResponse).collect(Collectors.toList());
    }

    // ---- Private helpers ----

    private BigDecimal hitungAngsuran(BigDecimal pokok, BigDecimal bungaPerBulan, int tenor) {
        BigDecimal r      = bungaPerBulan.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal oneR   = BigDecimal.ONE.add(r);
        BigDecimal oneRPow = oneR.pow(tenor);
        BigDecimal pembilang = pokok.multiply(r).multiply(oneRPow);
        BigDecimal penyebut  = oneRPow.subtract(BigDecimal.ONE);
        return pembilang.divide(penyebut, 2, RoundingMode.HALF_UP);
    }

    private List<AngsuranPinjaman> generateJadwalAngsuran(Pinjaman pinjaman) {
        List<AngsuranPinjaman> jadwal = new ArrayList<>();
        BigDecimal r = pinjaman.getBungaPerBulan().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal sisaPokok = pinjaman.getJumlahPinjaman();

        for (int i = 1; i <= pinjaman.getTenorBulan(); i++) {
            BigDecimal bunga = sisaPokok.multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pokok = (i == pinjaman.getTenorBulan())
                    ? sisaPokok
                    : pinjaman.getAngsuranPerBulan().subtract(bunga);

            jadwal.add(AngsuranPinjaman.builder()
                    .pinjaman(pinjaman)
                    .periodeKe(i)
                    .jumlahAngsuran(pokok.add(bunga))
                    .pokok(pokok)
                    .bunga(bunga)
                    .tanggalJatuhTempo(LocalDate.now().plusMonths(i))
                    .status(AngsuranPinjaman.StatusAngsuran.BELUM_BAYAR)
                    .build());

            sisaPokok = sisaPokok.subtract(pokok);
        }
        return jadwal;
    }

    private String formatRupiah(BigDecimal amount) {
        if (amount == null) return "0";
        return NumberFormat.getNumberInstance(new Locale("id", "ID")).format(amount);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
    }

    // Terima user sebagai parameter eksplisit — tidak bergantung pada lazy proxy
    private PinjamanDto.PinjamanResponse mapToResponse(Pinjaman p, User user, boolean includeAngsuran) {
        List<PinjamanDto.AngsuranResponse> angsuranList = null;
        if (includeAngsuran && p.getAngsuranList() != null) {
            angsuranList = p.getAngsuranList().stream()
                    .map(this::mapAngsuranToResponse)
                    .collect(Collectors.toList());
        }

        return PinjamanDto.PinjamanResponse.builder()
                .id(p.getId())
                .noPinjaman(p.getNoPinjaman())
                .userId(user.getId())
                .namaAnggota(user.getNamaLengkap())
                .nomorAnggota(user.getNomorAnggota())
                .jumlahPinjaman(p.getJumlahPinjaman())
                .bungaPerBulan(p.getBungaPerBulan())
                .tenorBulan(p.getTenorBulan())
                .angsuranPerBulan(p.getAngsuranPerBulan())
                .totalSudahDibayar(p.getTotalSudahDibayar())
                .sisaPinjaman(p.getSisaPinjaman())
                .tanggalPengajuan(p.getTanggalPengajuan())
                .tanggalDisetujui(p.getTanggalDisetujui())
                .tanggalJatuhTempo(p.getTanggalJatuhTempo())
                .tujuanPinjaman(p.getTujuanPinjaman())
                .status(p.getStatus().name())
                .keteranganAdmin(p.getKeteranganAdmin())
                .angsuranList(angsuranList)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PinjamanDto.AngsuranResponse mapAngsuranToResponse(AngsuranPinjaman a) {
        return PinjamanDto.AngsuranResponse.builder()
                .id(a.getId())
                .periodeKe(a.getPeriodeKe())
                .jumlahAngsuran(a.getJumlahAngsuran())
                .pokok(a.getPokok())
                .bunga(a.getBunga())
                .tanggalJatuhTempo(a.getTanggalJatuhTempo())
                .tanggalBayar(a.getTanggalBayar())
                .status(a.getStatus().name())
                .noReferensiBayar(a.getNoReferensiBayar())
                .build();
    }
}