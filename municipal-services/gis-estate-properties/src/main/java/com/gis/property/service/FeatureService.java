package com.gis.property.service;


import com.gis.property.model.Feature;
import com.gis.property.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;

    public Feature saveFeature(String type, String name, String wktGeometry) throws ParseException {
        WKTReader reader = new WKTReader();
        Geometry geometry = reader.read(wktGeometry);
        // Explicitly assign SRID 4326 in Java
        geometry.setSRID(4326);

        // Compute the centroid
        Geometry centroid;
        if (geometry instanceof Point) {
            centroid = geometry; // If it's already a point, store as is
        } else {
            centroid = geometry.getCentroid(); // Compute centroid for Polygons, LineStrings, etc.
        }

        // Extract latitude & longitude
        Double longitude = centroid.getCoordinate().x;
        Double latitude = centroid.getCoordinate().y;

        Feature feature = new Feature(null, type, name, latitude, longitude, geometry);

        return featureRepository.save(feature);
    }

    public List<Feature> getAllFeatures() {
        return featureRepository.findAll();
    }
}
