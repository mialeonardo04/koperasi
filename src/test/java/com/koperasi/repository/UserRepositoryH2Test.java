package com.koperasi.repository;

import com.koperasi.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
public class UserRepositoryH2Test {

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_thenFindByEmail() {
        User user = User.builder()
                .nomorAnggota("MBR-9999")
                .namaLengkap("Tester")
                .email("tester@email.com")
                .password("x")
                .role(User.Role.MEMBER)
                .status(User.StatusAnggota.AKTIF)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("tester@email.com");
        assertTrue(found.isPresent());
        assertEquals("Tester", found.get().getNamaLengkap());
    }
}
