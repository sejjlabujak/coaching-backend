package com.coaching_app.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String storeImage(MultipartFile file, String subdir) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Please upload an image file.");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadDir, subdir);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }

        return subdir + "/" + filename;
    }
}
