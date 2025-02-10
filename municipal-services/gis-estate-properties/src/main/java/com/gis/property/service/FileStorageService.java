package com.gis.property.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${upload.directory}")
    private String baseUploadDirectory;

    /**
     * Stores the uploaded file and returns the relative path.
     *
     * @param file The uploaded file.
     * @return The relative file path of the stored file.
     * @throws IOException If an error occurs during file storage.
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        // Generate a UUID-based filename
        String uuid = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String filename = uuid + fileExtension;

        // Create folder structure: year/month/day
        LocalDate today = LocalDate.now();
        Path uploadPath = Paths.get(baseUploadDirectory,
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()));

        // Ensure the directory structure exists
        Files.createDirectories(uploadPath);

        // Save the file
        File destinationFile = new File(uploadPath.toFile(), filename);
        file.transferTo(destinationFile);

        // Return the relative file path
        return Paths.get(String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                filename).toString();
    }

    // Utility method to extract file extension
    private String getFileExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}
