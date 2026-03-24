package com.koperasi.service;

import com.koperasi.dto.SimpananDto;
import com.koperasi.entity.Simpanan;
import com.koperasi.entity.User;
import com.koperasi.repository.SimpananRepository;
import com.koperasi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpananService {

    private final SimpananRepository simpananRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SimpananDto.SimpananResponse setor(Long userId, SimpananDto.SetorRequest request) {
        User user = getUser(userId);

        Simpanan simpanan = Simpanan.builder()
                .user(user)
                .jenis(request.getJenis())
                .jumlah(request.getJumlah())
                .tipe(Simpanan.TipeTransaksi.SETOR)
                .keterangan(request.getKeterangan())
                .tanggalTransaksi(LocalDateTime.now())
                .build();

        simpanan = simpananRepository.save(simpanan);

        BigDecimal totalBaru = simpananRepository.getTotalSimpananByUser(userId);
        user.setTotalSimpanan(totalBaru);
        userRepository.save(user);

        log.info("Setor simpanan {} - {} oleh user {}", request.getJenis(), request.getJumlah(), userId);

        // Audit Log
        auditLogService.log(
                null,
                "DEPOSIT",
                "simpanan",
                simpanan.getId().toString(),
                null,
                String.format("jenis: %s, jumlah: %s, target_user: %s", simpanan.getJenis(), simpanan.getJumlah(), user.getEmail())
        );

        return mapToResponse(simpanan, user);
    }

    @Transactional
    public SimpananDto.SimpananResponse tarik(Long userId, SimpananDto.TarikRequest request) {
        User user = getUser(userId);

        if (request.getJenis() == Simpanan.JenisSimpanan.POKOK) {
            throw new IllegalArgumentException("Simpanan Pokok tidak dapat ditarik selama masih menjadi anggota");
        }

        BigDecimal saldo = simpananRepository.getSaldoByUserAndJenis(userId, request.getJenis());
        if (saldo.compareTo(request.getJumlah()) < 0) {
            throw new IllegalArgumentException(
                    "Saldo " + request.getJenis() + " tidak cukup. Saldo saat ini: Rp " + saldo);
        }

        Simpanan simpanan = Simpanan.builder()
                .user(user)
                .jenis(request.getJenis())
                .jumlah(request.getJumlah())
                .tipe(Simpanan.TipeTransaksi.TARIK)
                .keterangan(request.getKeterangan())
                .tanggalTransaksi(LocalDateTime.now())
                .build();

        simpanan = simpananRepository.save(simpanan);

        BigDecimal totalBaru = simpananRepository.getTotalSimpananByUser(userId);
        user.setTotalSimpanan(totalBaru);
        userRepository.save(user);

        log.info("Tarik simpanan {} - {} oleh user {}", request.getJenis(), request.getJumlah(), userId);

        // Audit Log
        auditLogService.log(
                null,
                "WITHDRAW",
                "simpanan",
                simpanan.getId().toString(),
                null,
                String.format("jenis: %s, jumlah: %s, target_user: %s", simpanan.getJenis(), simpanan.getJumlah(), user.getEmail())
        );

        return mapToResponse(simpanan, user);
    }

    @Transactional(readOnly = true)
    public Page<SimpananDto.SimpananResponse> getRiwayat(Long userId, Pageable pageable) {
        return simpananRepository.findByUserIdOrderByTanggalTransaksiDesc(userId, pageable)
                .map(s -> mapToResponse(s, s.getUser()));
    }

    /**
     * Dipakai oleh admin untuk melihat semua transaksi.
     * Query dipisah berdasarkan kombinasi filter agar tidak ada parameter nullable
     * yang menyebabkan bug "could not determine data type" di PostgreSQL.
     */
    @Transactional(readOnly = true)
    public Page<SimpananDto.SimpananResponse> getAllSimpanan(Long userId, LocalDateTime dari,
                                                             LocalDateTime sampai, Pageable pageable) {
        boolean hasUser  = userId != null;
        boolean hasRange = dari != null && sampai != null;

        Page<Simpanan> result;

        if (hasUser && hasRange) {
            result = simpananRepository.findByUserIdAndDateRange(userId, dari, sampai, pageable);
        } else if (hasUser) {
            result = simpananRepository.findByUserId(userId, pageable);
        } else if (hasRange) {
            result = simpananRepository.findByDateRange(dari, sampai, pageable);
        } else {
            result = simpananRepository.findAllWithUser(pageable);
        }

        return result.map(s -> mapToResponse(s, s.getUser()));
    }

    public SimpananDto.SaldoResponse getSaldo(Long userId) {
        BigDecimal pokok    = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.POKOK);
        BigDecimal wajib    = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.WAJIB);
        BigDecimal sukarela = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.SUKARELA);

        return SimpananDto.SaldoResponse.builder()
                .simpananPokok(pokok)
                .simpananWajib(wajib)
                .simpananSukarela(sukarela)
                .totalSimpanan(pokok.add(wajib).add(sukarela))
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
    }

    private SimpananDto.SimpananResponse mapToResponse(Simpanan s, User user) {
        return SimpananDto.SimpananResponse.builder()
                .id(s.getId())
                .userId(user.getId())
                .namaAnggota(user.getNamaLengkap())
                .nomorAnggota(user.getNomorAnggota())
                .jenis(s.getJenis().name())
                .jumlah(s.getJumlah())
                .tipe(s.getTipe().name())
                .keterangan(s.getKeterangan())
                .tanggalTransaksi(s.getTanggalTransaksi())
                .noReferensi(s.getNoReferensi())
                .build();
    }
}