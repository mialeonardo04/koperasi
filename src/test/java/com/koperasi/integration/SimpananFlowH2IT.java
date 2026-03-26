package com.koperasi.integration;

import com.koperasi.dto.SimpananDto;
import com.koperasi.entity.Simpanan;
import com.koperasi.entity.User;
import com.koperasi.repository.AuditLogRepository;
import com.koperasi.repository.UserRepository;
import com.koperasi.service.SimpananService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:koperasi_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
public class SimpananFlowH2IT {

    @Autowired
    private SimpananService simpananService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void setorLaluTarik_mengubahSaldoDanMembuatAuditLog() {
        User user = userRepository.save(User.builder()
                .nomorAnggota("MBR-1001")
                .namaLengkap("Tester IT")
                .email("tester.it@koperasi.id")
                .password("x")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build());

        SimpananDto.SetorRequest setor = new SimpananDto.SetorRequest();
        setor.setJenis(Simpanan.JenisSimpanan.WAJIB);
        setor.setJumlah(new BigDecimal("100000"));
        setor.setKeterangan("IT setor");

        simpananService.setor(user.getId(), setor);

        SimpananDto.TarikRequest tarik = new SimpananDto.TarikRequest();
        tarik.setJenis(Simpanan.JenisSimpanan.WAJIB);
        tarik.setJumlah(new BigDecimal("50000"));
        tarik.setKeterangan("IT tarik");

        simpananService.tarik(user.getId(), tarik);

        SimpananDto.SaldoResponse saldo = simpananService.getSaldo(user.getId());
        assertEquals(0, saldo.getSimpananPokok().compareTo(BigDecimal.ZERO));
        assertEquals(0, saldo.getSimpananSukarela().compareTo(BigDecimal.ZERO));
        assertEquals(0, saldo.getSimpananWajib().compareTo(new BigDecimal("50000")));
        assertEquals(0, saldo.getTotalSimpanan().compareTo(new BigDecimal("50000")));

        assertTrue(auditLogRepository.count() >= 2);
    }
}
