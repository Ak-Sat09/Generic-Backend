package com.example.CouponVoucherService.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/uploads/chat")
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:*}")
public class ChatMediaController {
    @Value("${app.chat-media.directory:uploads/chat}")
    private String mediaDirectory;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getMedia(@PathVariable String filename) {
        Path root = Paths.get(mediaDirectory).toAbsolutePath().normalize();
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root)) return ResponseEntity.badRequest().build();
        if (!Files.isRegularFile(file)) return ResponseEntity.notFound().build();
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(detectMediaType(file))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    private MediaType detectMediaType(Path file) {
        try {
            String detected = Files.probeContentType(file);
            return detected == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(detected);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
