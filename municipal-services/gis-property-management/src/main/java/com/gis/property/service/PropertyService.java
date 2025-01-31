
package com.gis.property.service;

import com.gis.property.entity.Property;
import com.gis.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PropertyService {
    private final PropertyRepository repository;

    public PropertyService(PropertyRepository repository) {
        this.repository = repository;
    }

    public List<Property> getAllProperties() {
        return repository.findAll();
    }

    public Property saveProperty(Property property) {
        return repository.save(property);
    }

    public Property getPropertyById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public void deleteProperty(Long id) {
        repository.deleteById(id);
    }
}
