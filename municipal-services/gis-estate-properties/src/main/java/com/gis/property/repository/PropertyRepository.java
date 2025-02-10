package com.gis.property.repository;

import com.gis.property.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("SELECT p FROM Property p WHERE " +
            "LOWER(p.gisUid) LIKE %:search% OR " +
            "LOWER(p.name) LIKE %:search% OR " +
            "LOWER(p.bungalowNo) LIKE %:search%")
    Page<Property> findBySearchTerm(@Param("search") String search, Pageable pageable);
}
