package com.gis.property.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${upload.directory}")
    private String uploadDir;

    @GetMapping("/**")
    public ResponseEntity<Resource> servePicture(HttpServletRequest request) {
        String path = "";
        try {
            // Resolve the full file path
            path = request.getRequestURI().substring("/file/".length());
            Path filePath = Paths.get(uploadDir).resolve(path);
            // Load the file as a resource
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found: " + filePath.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error serving file for property : " + path, e);
        }
    }
}
