package com.koperasi.controller;

import com.koperasi.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileUploadController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "app.upload.dir=target/test-uploads/bukti-bayar"
})
public class FileUploadControllerWebMvcTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadBuktiBayar_returnsRelativePath() throws Exception {
        User principal = User.builder().id(1L).role(User.Role.MEMBER).build();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bukti.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/upload/bukti-bayar")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void viewBuktiBayar_whenPathTraversal_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/upload/bukti-bayar/view")
                        .param("path", "../secret.txt"))
                .andExpect(status().isBadRequest());
    }
}
