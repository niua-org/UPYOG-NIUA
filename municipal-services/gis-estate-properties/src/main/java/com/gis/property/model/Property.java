package com.gis.property.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Property extends AbstractModel<Long> {

    @Column(length = 50, nullable = true)
    private String gisUid;

    @Column(length = 100, nullable = true)
    private String roadName;

    @Column(length = 50, nullable = true)
    private String bungalowNo;

    @Column(length = 50, nullable = true)
    private String type;

    @Column(length = 50, nullable = true)
    private String permanentPool;

    @Column(length = 50, nullable = true)
    private String allotmentPool;

    @Column(length = 100, nullable = true)
    private String name;

    @Column(length = 100, nullable = true)
    private String designation;

    @Column(precision = 10,  nullable = true)
    private Double plotArea;

    @Column(precision = 10,  nullable = true)
    private Double plinthArea;

    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    @Column(length = 255, nullable = true)
    private String picture;

    @Column(length = 50, nullable = true)
    private String PID;

    @Column(length = 50, nullable = true)
    private String HID;

}
