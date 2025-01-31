
package com.gis.property.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String gisUid;
    private String roadName;
    private String bungalowNo;
    private String type;
    private String permanentPool;
    private String allotmentPool;
    private String name;
    private String designation;
    private Double plotArea;
    private Double plinthArea;

    // Getters and Setters
}
