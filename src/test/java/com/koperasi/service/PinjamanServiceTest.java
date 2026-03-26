package com.koperasi.service;

import com.koperasi.dto.PinjamanDto;
import com.koperasi.entity.AngsuranPinjaman;
import com.koperasi.entity.Pinjaman;
import com.koperasi.entity.User;
import com.koperasi.repository.AngsuranPinjamanRepository;
import com.koperasi.repository.PinjamanRepository;
import com.koperasi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PinjamanServiceTest {

    @Mock
    private PinjamanRepository pinjamanRepository;

    @Mock
    private AngsuranPinjamanRepository angsuranPinjamanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TelegramService telegramService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PinjamanService pinjamanService;

    @Test
    void ajukanPinjaman_whenHasActiveLoan_throws() {
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
        when(pinjamanRepository.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(true);

        PinjamanDto.PengajuanRequest req = new PinjamanDto.PengajuanRequest();
        req.setJumlahPinjaman(new BigDecimal("5000000.00"));
        req.setTenorBulan(12);
        req.setTujuanPinjaman("Modal");

        assertThrows(IllegalStateException.class, () -> pinjamanService.ajukanPinjaman(userId, req));
        verify(pinjamanRepository, never()).save(any());
    }

    @Test
    void ajukanPinjaman_savesPinjamanAndReturnsResponse() {
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
        when(pinjamanRepository.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(false);
        when(pinjamanRepository.save(any(Pinjaman.class))).thenAnswer(inv -> {
            Pinjaman p = inv.getArgument(0);
            p.setId(10L);
            p.setNoPinjaman("PIN-TEST");
            return p;
        });

        PinjamanDto.PengajuanRequest req = new PinjamanDto.PengajuanRequest();
        req.setJumlahPinjaman(new BigDecimal("5000000.00"));
        req.setTenorBulan(12);
        req.setTujuanPinjaman("Modal");

        PinjamanDto.PinjamanResponse res = pinjamanService.ajukanPinjaman(userId, req);

        assertEquals(10L, res.getId());
        assertEquals(userId, res.getUserId());
        assertEquals("PENDING", res.getStatus());
        assertEquals(new BigDecimal("5000000.00"), res.getSisaPinjaman());
        assertNotNull(res.getAngsuranPerBulan());
        assertEquals(2, res.getAngsuranPerBulan().scale());
    }

    @Test
    void prosesPersetujuan_whenApproved_generatesScheduleAndWritesAuditLog() {
        Long adminId = 99L;
        User admin = User.builder()
                .id(adminId)
                .email("admin@koperasi.id")
                .namaLengkap("Admin")
                .role(User.Role.ADMIN)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Long memberId = 2L;
        User memberRef = User.builder().id(memberId).build();
        User member = User.builder()
                .id(memberId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0002")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Pinjaman pinjaman = Pinjaman.builder()
                .id(5L)
                .user(memberRef)
                .jumlahPinjaman(new BigDecimal("5000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(6)
                .angsuranPerBulan(new BigDecimal("900000.00"))
                .sisaPinjaman(new BigDecimal("5000000.00"))
                .status(Pinjaman.StatusPinjaman.PENDING)
                .build();

        when(pinjamanRepository.findById(5L)).thenReturn(Optional.of(pinjaman));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(pinjamanRepository.save(any(Pinjaman.class))).thenAnswer(inv -> inv.getArgument(0));

        PinjamanDto.ApprovalRequest req = new PinjamanDto.ApprovalRequest();
        req.setDisetujui(true);
        req.setKeteranganAdmin("OK");
        req.setBungaPerBulan(new BigDecimal("2.00"));

        PinjamanDto.PinjamanResponse res = pinjamanService.prosesPersetujuan(5L, admin, req);

        assertEquals("DISETUJUI", res.getStatus());
        assertNotNull(pinjaman.getAngsuranList());
        assertEquals(6, pinjaman.getAngsuranList().size());

        verify(auditLogService).log(
                eq(admin),
                eq("APPROVE_LOAN"),
                eq("pinjaman"),
                eq("5"),
                eq("status: PENDING"),
                contains("status: DISETUJUI")
        );

        verifyNoInteractions(angsuranPinjamanRepository);
        verifyNoInteractions(telegramService);
    }

    @Test
    void prosesPersetujuan_whenRejected_writesAuditLog() {
        User admin = User.builder()
                .id(99L)
                .email("admin@koperasi.id")
                .namaLengkap("Admin")
                .role(User.Role.ADMIN)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Long memberId = 2L;
        User memberRef = User.builder().id(memberId).build();
        User member = User.builder()
                .id(memberId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0002")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Pinjaman pinjaman = Pinjaman.builder()
                .id(5L)
                .user(memberRef)
                .jumlahPinjaman(new BigDecimal("5000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(6)
                .angsuranPerBulan(new BigDecimal("900000.00"))
                .sisaPinjaman(new BigDecimal("5000000.00"))
                .status(Pinjaman.StatusPinjaman.PENDING)
                .build();

        when(pinjamanRepository.findById(5L)).thenReturn(Optional.of(pinjaman));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(pinjamanRepository.save(any(Pinjaman.class))).thenAnswer(inv -> inv.getArgument(0));

        PinjamanDto.ApprovalRequest req = new PinjamanDto.ApprovalRequest();
        req.setDisetujui(false);
        req.setKeteranganAdmin("Tolak");

        PinjamanDto.PinjamanResponse res = pinjamanService.prosesPersetujuan(5L, admin, req);

        assertEquals("DITOLAK", res.getStatus());
        verify(auditLogService).log(
                eq(admin),
                eq("REJECT_LOAN"),
                eq("pinjaman"),
                eq("5"),
                eq("status: PENDING"),
                contains("status: DITOLAK")
        );
    }

    @Test
    void bayarAngsuran_marksAngsuranPaid_andSetLoanLunasWhenAllPaid() {
        Long userId = 1L;
        User memberRef = User.builder().id(userId).build();
        User member = User.builder()
                .id(userId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0001")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Pinjaman pinjaman = Pinjaman.builder()
                .id(7L)
                .user(memberRef)
                .jumlahPinjaman(new BigDecimal("1000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(2)
                .angsuranPerBulan(new BigDecimal("510000.00"))
                .sisaPinjaman(new BigDecimal("500000.00"))
                .totalSudahDibayar(new BigDecimal("510000.00"))
                .status(Pinjaman.StatusPinjaman.DISETUJUI)
                .build();

        AngsuranPinjaman paid = AngsuranPinjaman.builder()
                .id(1L)
                .pinjaman(pinjaman)
                .periodeKe(1)
                .jumlahAngsuran(new BigDecimal("510000.00"))
                .pokok(new BigDecimal("500000.00"))
                .bunga(new BigDecimal("10000.00"))
                .tanggalJatuhTempo(LocalDate.now().minusDays(30))
                .status(AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR)
                .build();

        AngsuranPinjaman target = AngsuranPinjaman.builder()
                .id(2L)
                .pinjaman(pinjaman)
                .periodeKe(2)
                .jumlahAngsuran(new BigDecimal("510000.00"))
                .pokok(new BigDecimal("500000.00"))
                .bunga(new BigDecimal("10000.00"))
                .tanggalJatuhTempo(LocalDate.now())
                .status(AngsuranPinjaman.StatusAngsuran.BELUM_BAYAR)
                .build();

        pinjaman.getAngsuranList().addAll(List.of(paid, target));

        when(angsuranPinjamanRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(userId)).thenReturn(Optional.of(member));
        when(angsuranPinjamanRepository.save(any(AngsuranPinjaman.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pinjamanRepository.save(any(Pinjaman.class))).thenAnswer(inv -> inv.getArgument(0));

        PinjamanDto.BayarAngsuranRequest req = new PinjamanDto.BayarAngsuranRequest();
        req.setAngsuranId(2L);

        PinjamanDto.PinjamanResponse res = pinjamanService.bayarAngsuran(userId, req);

        assertEquals("LUNAS", res.getStatus());
        assertEquals(BigDecimal.ZERO, res.getSisaPinjaman());
        assertEquals(AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR, target.getStatus());
        assertNotNull(target.getTanggalBayar());
        assertNotNull(target.getNoReferensiBayar());
    }

    @Test
    void prosesPersetujuan_whenAlreadyProcessed_throws() {
        User admin = User.builder()
                .id(99L)
                .email("admin@koperasi.id")
                .namaLengkap("Admin")
                .role(User.Role.ADMIN)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Pinjaman pinjaman = Pinjaman.builder()
                .id(5L)
                .user(User.builder().id(2L).build())
                .jumlahPinjaman(new BigDecimal("5000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(6)
                .angsuranPerBulan(new BigDecimal("900000.00"))
                .sisaPinjaman(new BigDecimal("5000000.00"))
                .status(Pinjaman.StatusPinjaman.DISETUJUI)
                .build();

        when(pinjamanRepository.findById(5L)).thenReturn(Optional.of(pinjaman));

        PinjamanDto.ApprovalRequest req = new PinjamanDto.ApprovalRequest();
        req.setDisetujui(true);

        assertThrows(IllegalStateException.class, () -> pinjamanService.prosesPersetujuan(5L, admin, req));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getDetailPinjaman_whenNotOwnerAndNotAdmin_throws() {
        Long pinjamanId = 5L;
        Long ownerId = 2L;
        Long otherUserId = 99L;

        Pinjaman pinjaman = Pinjaman.builder()
                .id(pinjamanId)
                .user(User.builder().id(ownerId).build())
                .jumlahPinjaman(new BigDecimal("1000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(2)
                .angsuranPerBulan(new BigDecimal("510000.00"))
                .sisaPinjaman(new BigDecimal("1000000.00"))
                .status(Pinjaman.StatusPinjaman.PENDING)
                .build();

        when(pinjamanRepository.findById(pinjamanId)).thenReturn(Optional.of(pinjaman));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(User.builder().id(ownerId).build()));

        assertThrows(IllegalStateException.class, () -> pinjamanService.getDetailPinjaman(pinjamanId, otherUserId, false));
    }

    @Test
    void getAllPinjaman_whenStatusInvalid_callsRepoWithNullStatusEnum() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(pinjamanRepository.findWithFilter(isNull(), isNull(), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<PinjamanDto.PinjamanResponse> page = pinjamanService.getAllPinjaman(null, "not-a-status", pageable);

        assertEquals(0, page.getTotalElements());
        verify(pinjamanRepository).findWithFilter(isNull(), isNull(), eq(pageable));
    }

    @Test
    void getJadwalAngsuran_whenAdmin_returnsMappedList() {
        Long pinjamanId = 7L;
        Long ownerId = 2L;
        Pinjaman pinjaman = Pinjaman.builder()
                .id(pinjamanId)
                .user(User.builder().id(ownerId).build())
                .jumlahPinjaman(new BigDecimal("1000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(2)
                .angsuranPerBulan(new BigDecimal("510000.00"))
                .sisaPinjaman(new BigDecimal("1000000.00"))
                .status(Pinjaman.StatusPinjaman.DISETUJUI)
                .build();

        when(pinjamanRepository.findById(pinjamanId)).thenReturn(Optional.of(pinjaman));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(User.builder()
                .id(ownerId)
                .namaLengkap("Member")
                .nomorAnggota("MBR-0002")
                .build()));

        AngsuranPinjaman a1 = AngsuranPinjaman.builder()
                .id(1L)
                .pinjaman(pinjaman)
                .periodeKe(1)
                .jumlahAngsuran(new BigDecimal("510000.00"))
                .pokok(new BigDecimal("500000.00"))
                .bunga(new BigDecimal("10000.00"))
                .tanggalJatuhTempo(LocalDate.now())
                .status(AngsuranPinjaman.StatusAngsuran.BELUM_BAYAR)
                .build();

        when(angsuranPinjamanRepository.findByPinjamanIdOrderByPeriodeKe(pinjamanId)).thenReturn(List.of(a1));

        List<PinjamanDto.AngsuranResponse> list = pinjamanService.getJadwalAngsuran(pinjamanId, null, true);

        assertEquals(1, list.size());
        assertEquals(1, list.getFirst().getPeriodeKe());
        assertEquals("BELUM_BAYAR", list.getFirst().getStatus());
    }

    @Test
    void bayarAngsuran_whenNotOwner_throws() {
        Long userId = 1L;
        Pinjaman pinjaman = Pinjaman.builder()
                .id(7L)
                .user(User.builder().id(2L).build())
                .jumlahPinjaman(new BigDecimal("1000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(2)
                .angsuranPerBulan(new BigDecimal("510000.00"))
                .sisaPinjaman(new BigDecimal("1000000.00"))
                .status(Pinjaman.StatusPinjaman.DISETUJUI)
                .build();

        AngsuranPinjaman target = AngsuranPinjaman.builder()
                .id(2L)
                .pinjaman(pinjaman)
                .periodeKe(1)
                .jumlahAngsuran(new BigDecimal("510000.00"))
                .pokok(new BigDecimal("500000.00"))
                .bunga(new BigDecimal("10000.00"))
                .tanggalJatuhTempo(LocalDate.now())
                .status(AngsuranPinjaman.StatusAngsuran.BELUM_BAYAR)
                .build();

        when(angsuranPinjamanRepository.findById(2L)).thenReturn(Optional.of(target));

        PinjamanDto.BayarAngsuranRequest req = new PinjamanDto.BayarAngsuranRequest();
        req.setAngsuranId(2L);

        assertThrows(IllegalStateException.class, () -> pinjamanService.bayarAngsuran(userId, req));
    }

    @Test
    void bayarAngsuran_whenAlreadyPaid_throws() {
        Long userId = 1L;
        Pinjaman pinjaman = Pinjaman.builder()
                .id(7L)
                .user(User.builder().id(userId).build())
                .jumlahPinjaman(new BigDecimal("1000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(2)
                .angsuranPerBulan(new BigDecimal("510000.00"))
                .sisaPinjaman(new BigDecimal("1000000.00"))
                .status(Pinjaman.StatusPinjaman.DISETUJUI)
                .build();

        AngsuranPinjaman target = AngsuranPinjaman.builder()
                .id(2L)
                .pinjaman(pinjaman)
                .periodeKe(1)
                .jumlahAngsuran(new BigDecimal("510000.00"))
                .pokok(new BigDecimal("500000.00"))
                .bunga(new BigDecimal("10000.00"))
                .tanggalJatuhTempo(LocalDate.now())
                .status(AngsuranPinjaman.StatusAngsuran.SUDAH_BAYAR)
                .build();

        when(angsuranPinjamanRepository.findById(2L)).thenReturn(Optional.of(target));

        PinjamanDto.BayarAngsuranRequest req = new PinjamanDto.BayarAngsuranRequest();
        req.setAngsuranId(2L);

        assertThrows(IllegalStateException.class, () -> pinjamanService.bayarAngsuran(userId, req));
    }

    @Test
    void countPending_returnsRepositoryValue() {
        when(pinjamanRepository.countPending()).thenReturn(7L);
        assertEquals(7L, pinjamanService.countPending());
    }

    @Test
    void ajukanPinjaman_whenAdminChatConfigured_sendsTelegramToAdmin() {
        ReflectionTestUtils.setField(pinjamanService, "adminChatId", "123");

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
        when(pinjamanRepository.existsByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(false);
        when(pinjamanRepository.save(any(Pinjaman.class))).thenAnswer(inv -> {
            Pinjaman p = inv.getArgument(0);
            p.setId(10L);
            p.setNoPinjaman("PIN-TEST");
            return p;
        });

        PinjamanDto.PengajuanRequest req = new PinjamanDto.PengajuanRequest();
        req.setJumlahPinjaman(new BigDecimal("5000000.00"));
        req.setTenorBulan(12);
        req.setTujuanPinjaman("Modal");

        pinjamanService.ajukanPinjaman(userId, req);

        verify(telegramService).notifPinjamanBaru(
                eq("123"),
                eq("Member"),
                eq("MBR-0001"),
                anyString(),
                eq("Modal")
        );
    }

    @Test
    void prosesPersetujuan_whenApproved_andUserHasTelegramChatId_sendsTelegramToMember() {
        User admin = User.builder()
                .id(99L)
                .email("admin@koperasi.id")
                .namaLengkap("Admin")
                .role(User.Role.ADMIN)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Long memberId = 2L;
        User memberRef = User.builder().id(memberId).build();
        User member = User.builder()
                .id(memberId)
                .email("member@email.com")
                .namaLengkap("Member")
                .nomorAnggota("MBR-0002")
                .telegramChatId("456")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        Pinjaman pinjaman = Pinjaman.builder()
                .id(5L)
                .user(memberRef)
                .jumlahPinjaman(new BigDecimal("5000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(6)
                .angsuranPerBulan(new BigDecimal("900000.00"))
                .sisaPinjaman(new BigDecimal("5000000.00"))
                .status(Pinjaman.StatusPinjaman.PENDING)
                .build();

        when(pinjamanRepository.findById(5L)).thenReturn(Optional.of(pinjaman));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(pinjamanRepository.save(any(Pinjaman.class))).thenAnswer(inv -> inv.getArgument(0));

        PinjamanDto.ApprovalRequest req = new PinjamanDto.ApprovalRequest();
        req.setDisetujui(true);
        req.setKeteranganAdmin("OK");

        pinjamanService.prosesPersetujuan(5L, admin, req);

        verify(telegramService).notifPinjamanDisetujui(
                eq("456"),
                eq("Member"),
                isNull(),
                anyString(),
                eq("6"),
                anyString()
        );
    }

    @Test
    void getAllPinjaman_whenStatusValid_callsRepoWithEnum() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(pinjamanRepository.findWithFilter(isNull(), eq(Pinjaman.StatusPinjaman.PENDING), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<PinjamanDto.PinjamanResponse> page = pinjamanService.getAllPinjaman(null, "pending", pageable);

        assertEquals(0, page.getTotalElements());
        verify(pinjamanRepository).findWithFilter(isNull(), eq(Pinjaman.StatusPinjaman.PENDING), eq(pageable));
    }

    @Test
    void getJadwalAngsuran_whenOwner_returnsMappedList() {
        Long pinjamanId = 7L;
        Long ownerId = 2L;
        Pinjaman pinjaman = Pinjaman.builder()
                .id(pinjamanId)
                .user(User.builder().id(ownerId).build())
                .jumlahPinjaman(new BigDecimal("1000000.00"))
                .bungaPerBulan(new BigDecimal("1.50"))
                .tenorBulan(2)
                .angsuranPerBulan(new BigDecimal("510000.00"))
                .sisaPinjaman(new BigDecimal("1000000.00"))
                .status(Pinjaman.StatusPinjaman.DISETUJUI)
                .build();

        when(pinjamanRepository.findById(pinjamanId)).thenReturn(Optional.of(pinjaman));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(User.builder()
                .id(ownerId)
                .namaLengkap("Member")
                .nomorAnggota("MBR-0002")
                .build()));

        AngsuranPinjaman a1 = AngsuranPinjaman.builder()
                .id(1L)
                .pinjaman(pinjaman)
                .periodeKe(1)
                .jumlahAngsuran(new BigDecimal("510000.00"))
                .pokok(new BigDecimal("500000.00"))
                .bunga(new BigDecimal("10000.00"))
                .tanggalJatuhTempo(LocalDate.now())
                .status(AngsuranPinjaman.StatusAngsuran.BELUM_BAYAR)
                .build();

        when(angsuranPinjamanRepository.findByPinjamanIdOrderByPeriodeKe(pinjamanId)).thenReturn(List.of(a1));

        List<PinjamanDto.AngsuranResponse> list = pinjamanService.getJadwalAngsuran(pinjamanId, ownerId, false);

        assertEquals(1, list.size());
        assertEquals(1, list.getFirst().getPeriodeKe());
    }
}
