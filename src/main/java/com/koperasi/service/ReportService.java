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

        List<com.koperasi.entity.Pinjaman> pinjamanAktifList = pinjamanRepository
                .findByUserIdAndStatus(userId, com.koperasi.entity.Pinjaman.StatusPinjaman.DISETUJUI);

        BigDecimal totalPinjaman = pinjamanAktifList.stream()
                .map(com.koperasi.entity.Pinjaman::getJumlahPinjaman)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sisaPinjaman = pinjamanAktifList.stream()
                .map(com.koperasi.entity.Pinjaman::getSisaPinjaman)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long angsuranTerlambat = angsuranRepository.findAngsuranTerlambat(userId, LocalDate.now()).size();

        return ReportDto.DashboardMember.builder()
                .nomorAnggota(user.getNomorAnggota())
                .namaLengkap(user.getNamaLengkap())
                .simpananPokok(simpananPokok)
                .simpananWajib(simpananWajib)
                .simpananSukarela(simpananSukarela)
                .totalSimpanan(totalSimpanan)
                .pinjamanAktif((long) pinjamanAktifList.size())
                .totalPinjaman(totalPinjaman)
                .sisaPinjaman(sisaPinjaman)
                .angsuranTerlambat(angsuranTerlambat)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportDto.RekapSimpanan> getRekapSimpananAllMember() {
        return userRepository.findAllMembers(Pageable.unpaged())
                .stream()
                .map(user -> {
                    BigDecimal pokok    = simpananRepository.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.POKOK);
                    BigDecimal wajib    = simpananRepository.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.WAJIB);
                    BigDecimal sukarela = simpananRepository.getSaldoByUserAndJenis(user.getId(), Simpanan.JenisSimpanan.SUKARELA);
                    return ReportDto.RekapSimpanan.builder()
                            .nomorAnggota(user.getNomorAnggota())
                            .namaAnggota(user.getNamaLengkap())
                            .simpananPokok(pokok)
                            .simpananWajib(wajib)
                            .simpananSukarela(sukarela)
                            .total(pokok.add(wajib).add(sukarela))
                            .build();
                })
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