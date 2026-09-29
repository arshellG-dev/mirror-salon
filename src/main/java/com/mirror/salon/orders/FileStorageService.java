package com.mirror.salon.orders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    /** Saves the file and returns its web-servable path, e.g. /uploads/slips/&lt;uuid&gt;-name.jpg */
    public String storeSlip(MultipartFile file) {
        try {
            Path dir = Path.of(uploadsDir);
            Files.createDirectories(dir);
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "slip" : file.getOriginalFilename());
            String filename = UUID.randomUUID() + "-" + original;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return "/uploads/slips/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    /** Saves a stylist profile photo and returns its web-servable path, e.g. /uploads/photos/&lt;uuid&gt;-name.jpg */
    public String storePhoto(MultipartFile file) {
        try {
            Path dir = Path.of("uploads/photos");
            Files.createDirectories(dir);
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename());
            String filename = UUID.randomUUID() + "-" + original;
            file.transferTo(dir.resolve(filename));
            return "/uploads/photos/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }
}
