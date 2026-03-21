package com.koperasi.service;

import com.koperasi.dto.ReportDto;
import com.koperasi.entity.Simpanan;
import com.koperasi.entity.User;
import com.koperasi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final SimpananRepository simpananRepository;
    private final PinjamanRepository        pinjamanRepository;
    private final AngsuranPinjamanRepository angsuranRepository;
    private final PinjamanKelompokRepository pinjamanKelompokRepository;
    private final KelompokAnggotaRepository  kelompokAnggotaRepository;

    @Transactional(readOnly = true)
    public ReportDto.DashboardAdmin getDashboardAdmin() {
        long totalAnggota = userRepository.count();
        long anggotaAktif = userRepository.findAllMembers(Pageable.unpaged()).getTotalElements();
        BigDecimal totalSimpanan = simpananRepository.getTotalSetoranSemua()
                .subtract(simpananRepository.getTotalPenarikanSemua());
        // Gabung pinjaman individu + kelompok
        BigDecimal totalPinjamanAktif = pinjamanRepository.getTotalPinjamanAktif()
                .add(pinjamanKelompokRepository.getTotalPinjamanAktif());
        BigDecimal totalSisaPinjaman  = pinjamanRepository.getTotalSisaPinjaman()
                .add(pinjamanKelompokRepository.getTotalSisaPinjaman());
        long pinjamanPending    = pinjamanRepository.countPending()
                + pinjamanKelompokRepository.countPending();
        long pinjamanAktif      = pinjamanRepository.countAktif()
                + pinjamanKelompokRepository.countAktif();
        long angsuranJatuhTempo = angsuranRepository.countAngsuranJatuhTempo(LocalDate.now());

        return ReportDto.DashboardAdmin.builder()
                .totalAnggota(totalAnggota)
                .anggotaAktif(anggotaAktif)
                .totalSimpanan(totalSimpanan)
                .totalPinjamanAktif(totalPinjamanAktif)
                .totalSisaPinjaman(totalSisaPinjaman)
                .pinjamanPending(pinjamanPending)
                .pinjamanAktif(pinjamanAktif)
                .angsuranJatuhTempo(angsuranJatuhTempo)
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.DashboardMember getDashboardMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        BigDecimal simpananPokok    = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.POKOK);
        BigDecimal simpananWajib    = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.WAJIB);
        BigDecimal simpananSukarela = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.SUKARELA);
        BigDecimal totalSimpanan    = simpananPokok.add(simpananWajib).add(simpananSukarela);

        // Pinjaman individu aktif (tidak dipakai lagi, tapi tetap dihitung)
        List<com.koperasi.entity.Pinjaman> pinjamanAktifList = pinjamanRepository
                .findByUserIdAndStatus(userId, com.koperasi.entity.Pinjaman.StatusPinjaman.DISETUJUI);

        // Pinjaman kelompok — ambil dari kelompok yang user ini jadi anggota
        List<com.koperasi.entity.KelompokAnggota> keanggotaan = kelompokAnggotaRepository
                .findByUserId(userId);

        BigDecimal totalPinjamanKelompok = BigDecimal.ZERO;
        BigDecimal sisaPinjamanKelompok  = BigDecimal.ZERO;
        long jumlahPinjamanKelompok = 0;

        for (com.koperasi.entity.KelompokAnggota ka : keanggotaan) {
            if (ka.getKelompok().getStatus() != com.koperasi.entity.Kelompok.StatusKelompok.AKTIF) continue;
            // Cari pinjaman aktif kelompok ini
            java.util.Optional<com.koperasi.entity.PinjamanKelompok> pk =
                    pinjamanKelompokRepository.findActivePinjamanByKelompokId(ka.getKelompok().getId());
            if (pk.isPresent() && pk.get().getStatus() == com.koperasi.entity.PinjamanKelompok.StatusPinjaman.DISETUJUI) {
                com.koperasi.entity.PinjamanKelompok p = pk.get();
                totalPinjamanKelompok = totalPinjamanKelompok.add(p.getJumlahPinjaman());
                sisaPinjamanKelompok  = sisaPinjamanKelompok.add(p.getSisaPinjaman());
                jumlahPinjamanKelompok++;
                break; // satu user hanya bisa di satu kelompok aktif
            }
        }

        // Hitung persentase sisa pinjaman kelompok
        BigDecimal pctSisa = BigDecimal.ZERO;
        if (totalPinjamanKelompok.compareTo(BigDecimal.ZERO) > 0) {
            pctSisa = sisaPinjamanKelompok
                    .multiply(new BigDecimal("100"))
                    .divide(totalPinjamanKelompok, 2, java.math.RoundingMode.HALF_UP);
        }

        long angsuranTerlambat = angsuranRepository.findAngsuranTerlambat(userId, LocalDate.now()).size();

        return ReportDto.DashboardMember.builder()
                .nomorAnggota(user.getNomorAnggota())
                .namaLengkap(user.getNamaLengkap())
                .simpananPokok(simpananPokok)
                .simpananWajib(simpananWajib)
                .simpananSukarela(simpananSukarela)
                .totalSimpanan(totalSimpanan)
                .pinjamanAktif(jumlahPinjamanKelompok)
                .totalPinjaman(totalPinjamanKelompok)
                .sisaPinjaman(pctSisa)       // persentase sisa
                .angsuranTerlambat(angsuranTerlambat)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportDto.RekapSimpanan> getRekapSimpananAllMember() {
        return getRekapSimpananAllMember(0, 0);
    }

    @Transactional(readOnly = true)
    public List<ReportDto.RekapSimpanan> getRekapSimpananAllMember(int bulan, int tahun) {
        return userRepository.findAllMembers(Pageable.unpaged())
                .stream()
                .map(user -> {
                    BigDecimal pokok    = simpananRepository.getSaldoByUserAndJenisPeriode(user.getId(), Simpanan.JenisSimpanan.POKOK,    bulan, tahun);
                    BigDecimal wajib    = simpananRepository.getSaldoByUserAndJenisPeriode(user.getId(), Simpanan.JenisSimpanan.WAJIB,    bulan, tahun);
                    BigDecimal sukarela = simpananRepository.getSaldoByUserAndJenisPeriode(user.getId(), Simpanan.JenisSimpanan.SUKARELA, bulan, tahun);
                    BigDecimal total    = pokok.add(wajib).add(sukarela);
                    // Skip anggota dengan total 0 jika ada filter periode
                    if ((bulan != 0 || tahun != 0) && total.compareTo(BigDecimal.ZERO) == 0) return null;
                    return ReportDto.RekapSimpanan.builder()
                            .nomorAnggota(user.getNomorAnggota())
                            .namaAnggota(user.getNamaLengkap())
                            .simpananPokok(pokok)
                            .simpananWajib(wajib)
                            .simpananSukarela(sukarela)
                            .total(total)
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportDto.RekapPinjaman> getRekapPinjamanAllMember() {
        // Pakai findAllWithUser() agar user tidak lazy — menghindari LazyInitializationException
        return pinjamanRepository.findAllWithUser().stream()
                .map(p -> ReportDto.RekapPinjaman.builder()
                        .nomorAnggota(p.getUser().getNomorAnggota())
                        .namaAnggota(p.getUser().getNamaLengkap())
                        .noPinjaman(p.getNoPinjaman())
                        .jumlahPinjaman(p.getJumlahPinjaman())
                        .sudahDibayar(p.getTotalSudahDibayar())
                        .sisaPinjaman(p.getSisaPinjaman())
                        .status(p.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }
}