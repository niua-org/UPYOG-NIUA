package com.gis.property.repository.dto;

import lombok.Data;

@Data
public class FeatureRequest {
    private String type;
    private String name;
    private String wktGeometry;
}