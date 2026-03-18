package com.koperasi.controller;

import com.koperasi.dto.ApiResponse;
import com.koperasi.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
public class FileUploadController {

    // Membaca dari application.properties:  app.upload.dir=D:/koperasi/uploads/bukti-bayar
    @Value("${app.upload.dir:./uploads/bukti-bayar}")
    private String uploadDir;

    private static final long MAX_SIZE = 1024 * 1024L; // 1 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /**
     * Upload bukti pembayaran.
     * File disimpan di: {app.upload.dir}/{tahun}/{bulan}/{uuid}.{ext}
     * Mengembalikan path relatif yang disimpan ke database.
     *
     * Contoh path di Windows:
     *   D:/koperasi/uploads/bukti-bayar/2026/03/abc123.jpg
     * Yang disimpan ke DB (relatif):
     *   2026/03/abc123.jpg
     */
    @PostMapping("/bukti-bayar")
    public ResponseEntity<ApiResponse<String>> uploadBuktiBayar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File tidak boleh kosong"));
        }

        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ukuran file maksimal 1MB. Ukuran file saat ini: "
                            + String.format("%.2f", file.getSize() / 1024.0 / 1024.0) + "MB"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tipe file tidak didukung. Gunakan JPG, PNG, atau WebP"));
        }

        // Subfolder tahun/bulan → misal: 2026/03
        String subFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        // Paths.get() otomatis handle Windows separator
        Path targetDir = Paths.get(uploadDir).resolve(subFolder);
        Files.createDirectories(targetDir); // buat folder jika belum ada

        String ext      = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path   target   = targetDir.resolve(fileName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Simpan sebagai path dengan forward slash (lintas OS)
        String relativePath = subFolder + "/" + fileName;
        log.info("Upload bukti bayar by user {}: {} ({} bytes)", user.getId(), relativePath, file.getSize());

        return ResponseEntity.ok(ApiResponse.ok("Upload berhasil", relativePath));
    }

    /**
     * Serve file untuk ditampilkan di browser.
     * GET /api/upload/bukti-bayar/view?path=2026/03/abc123.jpg
     */
    @GetMapping("/bukti-bayar/view")
    public ResponseEntity<Resource> viewBuktiBayar(@RequestParam String path) {
        try {
            // Sanitasi — hindari path traversal attack
            if (path == null || path.contains("..") || path.startsWith("/") || path.startsWith("\\")) {
                return ResponseEntity.badRequest().build();
            }
            // Ganti backslash jika ada
            path = path.replace("\\", "/");

            Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(path).normalize();

            // Pastikan masih di dalam uploadDir
            if (!filePath.startsWith(rootPath)) {
                log.warn("Path traversal attempt: {}", path);
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Malformed URL for path: {}", path, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error serving file: {}", path, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        // Hanya izinkan ekstensi yang aman
        return Set.of("jpg", "jpeg", "png", "webp").contains(ext) ? ext : "jpg";
    }
}