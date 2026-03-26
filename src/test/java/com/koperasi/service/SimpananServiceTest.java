package com.koperasi.service;

import com.koperasi.dto.SimpananDto;
import com.koperasi.entity.Simpanan;
import com.koperasi.entity.User;
import com.koperasi.repository.SimpananRepository;
import com.koperasi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SimpananServiceTest {

    @Mock
    private SimpananRepository simpananRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SimpananService simpananService;

    @Test
    void setor_savesSimpanan_updatesUserTotal_andWritesAuditLog() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0001")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .totalSimpanan(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(simpananRepository.save(any(Simpanan.class))).thenAnswer(inv -> {
            Simpanan s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });
        when(simpananRepository.getTotalSimpananByUser(userId)).thenReturn(new BigDecimal("150000.00"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SimpananDto.SetorRequest req = new SimpananDto.SetorRequest();
        req.setJenis(Simpanan.JenisSimpanan.WAJIB);
        req.setJumlah(new BigDecimal("100000.00"));
        req.setKeterangan("Simpanan wajib");

        SimpananDto.SimpananResponse res = simpananService.setor(userId, req);

        assertEquals(10L, res.getId());
        assertEquals(userId, res.getUserId());
        assertEquals("WAJIB", res.getJenis());
        assertEquals("SETOR", res.getTipe());
        assertEquals(new BigDecimal("100000.00"), res.getJumlah());
        assertEquals(new BigDecimal("150000.00"), user.getTotalSimpanan());

        verify(auditLogService).log(
                isNull(),
                eq("DEPOSIT"),
                eq("simpanan"),
                eq("10"),
                isNull(),
                contains("jenis: WAJIB")
        );
    }

    @Test
    void tarik_whenJenisPokok_throws() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0001")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SimpananDto.TarikRequest req = new SimpananDto.TarikRequest();
        req.setJenis(Simpanan.JenisSimpanan.POKOK);
        req.setJumlah(new BigDecimal("10000.00"));

        assertThrows(IllegalArgumentException.class, () -> simpananService.tarik(userId, req));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void tarik_whenSaldoNotEnough_throws() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0001")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.SUKARELA)).thenReturn(new BigDecimal("5000.00"));

        SimpananDto.TarikRequest req = new SimpananDto.TarikRequest();
        req.setJenis(Simpanan.JenisSimpanan.SUKARELA);
        req.setJumlah(new BigDecimal("10000.00"));

        assertThrows(IllegalArgumentException.class, () -> simpananService.tarik(userId, req));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void tarik_savesSimpanan_updatesUserTotal_andWritesAuditLog() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0001")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .totalSimpanan(new BigDecimal("200000.00"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.SUKARELA)).thenReturn(new BigDecimal("150000.00"));
        when(simpananRepository.save(any(Simpanan.class))).thenAnswer(inv -> {
            Simpanan s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });
        when(simpananRepository.getTotalSimpananByUser(userId)).thenReturn(new BigDecimal("100000.00"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SimpananDto.TarikRequest req = new SimpananDto.TarikRequest();
        req.setJenis(Simpanan.JenisSimpanan.SUKARELA);
        req.setJumlah(new BigDecimal("50000.00"));
        req.setKeterangan("Tarik");

        SimpananDto.SimpananResponse res = simpananService.tarik(userId, req);

        assertEquals(11L, res.getId());
        assertEquals("TARIK", res.getTipe());
        assertEquals(new BigDecimal("100000.00"), user.getTotalSimpanan());

        verify(auditLogService).log(
                isNull(),
                eq("WITHDRAW"),
                eq("simpanan"),
                eq("11"),
                isNull(),
                contains("jenis: SUKARELA")
        );
    }

    @Test
    void getSaldo_aggregatesAllJenis() {
        Long userId = 1L;
        when(simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.POKOK)).thenReturn(new BigDecimal("500000.00"));
        when(simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.WAJIB)).thenReturn(new BigDecimal("1500000.00"));
        when(simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.SUKARELA)).thenReturn(new BigDecimal("250000.00"));

        SimpananDto.SaldoResponse res = simpananService.getSaldo(userId);

        assertEquals(new BigDecimal("500000.00"), res.getSimpananPokok());
        assertEquals(new BigDecimal("1500000.00"), res.getSimpananWajib());
        assertEquals(new BigDecimal("250000.00"), res.getSimpananSukarela());
        assertEquals(new BigDecimal("2250000.00"), res.getTotalSimpanan());
    }
}
