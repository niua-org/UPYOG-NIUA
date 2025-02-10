package com.gis.property.service;

import com.gis.property.model.Property;
import com.gis.property.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class PropertyService extends AbstractService<Property, Long> {

    private final FileStorageService fileStorageService;
    private final PropertyRepository propertyRepository;

    @Autowired
    public PropertyService(FileStorageService fileStorageService, PropertyRepository propertyRepository) {
        this.fileStorageService = fileStorageService;
        this.propertyRepository = propertyRepository;
    }

    @Override
    protected JpaRepository<Property, Long> getRepository() {
        return propertyRepository;
    }

    /**
     * Save a Property with file handling.
     *
     * @param property    The property to save.
     * @param pictureFile The file to upload, if any.
     * @return The saved property.
     */
    public Property save(Property property, MultipartFile pictureFile) {
        try {
            // Handle file upload
            if (pictureFile != null && !pictureFile.isEmpty()) {
                String relativePath = fileStorageService.storeFile(pictureFile);
                property.setPicture(relativePath);
            }

            // Delegate save to AbstractService or repository
            return super.save(property);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }


    /**
     * Update a Property with file handling.
     *
     * @param property    The property to update.
     * @param pictureFile The file to upload, if any.
     * @return The updated property.
     */
    public Property update(Property property, MultipartFile pictureFile) {
        try {
            // Handle file upload
            if (pictureFile != null && !pictureFile.isEmpty()) {
                String relativePath = fileStorageService.storeFile(pictureFile);
                property.setPicture(relativePath);
            }

            // Delegate update to AbstractService or repository
            return super.save(property);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public long getPropertiesCount() {
        return propertyRepository.count();
    }

    public List<Property> getAllProperties() {
        return getRepository().findAll();
    }

    public Page<Property> searchProperties(String search, int pageNumber) {
        int pageSize = 10; // 10 records per page
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        if (search == null || search.isEmpty()) {
            return propertyRepository.findAll(pageable);
        }

        // Example: Search across multiple fields
        return propertyRepository.findBySearchTerm(search.toLowerCase(), pageable);
    }
}
