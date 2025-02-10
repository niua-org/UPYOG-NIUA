package com.gis.property.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(name = "features")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    private Double latitude;  // Stores extracted latitude
    private Double longitude; // Stores extracted longitude

    // Store full geometry (Polygon, LineString, etc.)
    @Column(columnDefinition = "geometry(Geometry, 4326)", nullable = false)
    private Geometry geom;

}
