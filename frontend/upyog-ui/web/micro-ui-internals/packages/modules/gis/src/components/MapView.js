import React, { useEffect, useRef, useState } from 'react';
import { useTranslation } from "react-i18next";
import { CardLabel, SubmitBar, Dropdown } from '@upyog/digit-ui-react-components';

/**
This MapView defines the MapView component, which is responsible for rendering and managing the map interface in the GIS module.
It integrates with mapping libraries (e.g., Leaflet, OpenLayers, or similar) to display geospatial data.
The component provides functionalities for searching and filtering map data based on user input and show details in pop up.
Displaying user's current location on the map.
Rendering data points as markers with popup details.
Allowing users to search and filter map data by application number.
Calculating distances between user location and feature locations.
 */

const MapView = () => {
  const mapRef = useRef(null);
  const [userLocation, setUserLocation] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [geoJsonData, setGeoJsonData] = useState({ type: "FeatureCollection", features: [] });
  const { t } = useTranslation();
  
  const getBusinessService = () => {
    const selectedServiceType = sessionStorage.getItem('selectedServiceType');
    if (selectedServiceType) {
      const serviceType = JSON.parse(selectedServiceType);
      return serviceType.businessService || "PT";
    }
    return "PT"; // default fallback
  };

  //Fetches the selected business service type from sessionStorage.
  //Used to determine which GIS API to call (Property or Asset).
  useEffect(() => {
    const fetchGISData = async () => {
      try {
        const businessService = getBusinessService();
        const payload = {
          tenantId: "pg.citya",
          fromDate: 1743445800000,
          toDate: 1774981799000,
          includeBillData: true,
          businessService: businessService
        };

        const response = businessService === "PT"
          ? await Digit.GIS.searchPT(payload)
          : await Digit.GIS.searchAsset(payload);


        if (response && response.geoJsonData) {
          const validFeatures = response.geoJsonData.features.filter(features => {
            const coords = features.geometry?.coordinates;
            return (
              Array.isArray(coords) &&
              coords.length === 2 &&
              !(coords[0] === 0.0 && coords[1] === 0.0)
            );
          });

          const transformedData = {
            type: "FeatureCollection",
            features: validFeatures.map(features => ({
              type: "Feature",
              geometry: features.geometry,
              properties: businessService === "PT" ? {
                applicationNumber: features.properties.applicationNumber,
                propertyType: features.properties.propertyType,
                status: features.properties.status,
                paymentStatus: features.properties.paymentStatus,
                landArea: features.properties.landArea,
                usageCategory: features.properties.usageCategory,
                ownershipCategory: features.properties.ownershipCategory
              } : {
                applicationNumber: features.properties.applicationNumber,
                assetName: features.properties.assetName,
                assetDepartment: features.properties.department,
                assetCategory: features.properties.assetCategory,
                status: features.properties.status,
              }
            }))
          };

          setGeoJsonData(transformedData);
        }
      } catch (error) {
        console.error("Error fetching GIS data:", error);
      }
    };

    fetchGISData();
  }, []);


  /**
     * Prepares dropdown options based on fetched GIS data.
     */
  const statusOptions = geoJsonData.features.map(feature => ({
    code: feature.properties.applicationNumber,
    value: feature.properties.applicationNumber,
    i18nKey: feature.properties.applicationNumber
  }));

  const handleSearch = () => {
    setSearchTerm(inputValue?.value || inputValue || "");
  };
  // Utility function to calculate distance between two geographic coordinates using the Haversine formula.
  const calculateDistance = (lat1, lng1, lat2, lng2) => {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLng / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  /**
   * Attempts to get the user's current location via the browser's Geolocation API.
   */
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
        },
        (error) => {
          console.warn("Location access denied or unavailable:", error);
        }
      );
    }
  }, []);

  /**
 * Loads the Leaflet library dynamically (if not already loaded)
 * and initializes the map once both the user location and GIS data are ready.
 */
  useEffect(() => {
    if (!userLocation || !geoJsonData.features.length) return;

    const loadLeaflet = () => {
      if (!window.L) {
        const link = document.createElement('link');
        link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
        link.rel = 'stylesheet';
        document.head.appendChild(link);

        const script = document.createElement('script');
        script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        script.onload = initMap;
        document.head.appendChild(script);
      } else {
        initMap();
      }
    };

    loadLeaflet();
  }, [userLocation, geoJsonData, searchTerm]);

  const initMap = () => {
    if (!mapRef.current) return;

    if (mapRef.current._leaflet_id) {
      mapRef.current._leaflet_id = null;
      mapRef.current.innerHTML = "";
    }

    const map = window.L.map(mapRef.current).setView([userLocation.lat, userLocation.lng], 12);
    window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

    const userIcon = window.L.divIcon({
      html: '<div style="font-size: 24px; color: blue;">⦿</div>',
      iconSize: [40, 40],
      className: 'user-location-marker'
    });

    window.L.marker([userLocation.lat, userLocation.lng], { icon: userIcon })
      .addTo(map)
      .bindPopup('<b>Your Location</b>');

    const markerMap = new Map();

    // Plot GIS features as markers
    geoJsonData.features.forEach(feature => {
      const [lng, lat] = feature.geometry.coordinates;
      const props = feature.properties;
      const distance = calculateDistance(userLocation.lat, userLocation.lng, lat, lng);
      const marker = window.L.marker([lat, lng]).addTo(map);

      const businessService = getBusinessService();
      const popupContent = businessService === "PT" ? `
        <div>
          <div style="margin-right: 85px;">
            <b>${props.propertyType}</b><br>
            <b>Status:</b> ${props.status || "N/A"}<br>
            <b>Payment:</b> ${props.paymentStatus || "N/A"}<br>
            <b>Land Area:</b> ${props.landArea || "N/A"}<br>
            <b>Usage:</b> ${props.usageCategory || "N/A"}<br>
            <b>Distance:</b> ${distance.toFixed(1)} km<br>
          </div>
        </div>
      ` : `
        <div>
          <div style="margin-right: 85px;">
            <b>${props.assetName}</b><br>
            <b>Status:</b> ${props.status || "N/A"}<br>
            <b>Asset Category:</b> ${props.assetCategory || "N/A"}<br>
            <b>Asset Department:</b> ${props.assetDepartment || "N/A"}<br>
            <b>Distance:</b> ${distance.toFixed(1)} km<br>
          </div>
        </div>
      `;

      marker.bindPopup(popupContent);
      markerMap.set(props.applicationNumber.toLowerCase(), { marker, lat, lng });
    });

    if (searchTerm) {
      const term = searchTerm.trim().toLowerCase();
      for (let [key, { marker, lat, lng }] of markerMap) {
        if (key.includes(term)) {
          map.flyTo([lat, lng], 18);
          marker.openPopup();
          break;
        }
      }
    }
  };

  return (
    <div>
      <div style={{ marginLeft: "10px", marginRight: "10px" }}>
        <CardLabel>{t("GIS_PROPERTY_AVAILABLE")}</CardLabel>
        <div style={{ display: "flex", flexDirection: "row", alignItems: "flex-start", gap: "16px", position: "relative", zIndex: 2000 }}>
          <Dropdown
            className="form-field"
            selected={inputValue}
            select={setInputValue}
            option={statusOptions}
            placeholder={t("Select Property")}
            optionKey="i18nKey"
            style={{ width: "100%", position: "relative", zIndex: 2001 }}
            optionCardStyles={{ maxHeight: "200px", overflowY: "auto" }}
            t={t}
          />
          <div style={{ marginTop: "5px", display: "flex", gap: "16px" }}>
            <SubmitBar label={t("ES_COMMON_SEARCH")} onSubmit={handleSearch} />
            <p className="link" style={{ cursor: "pointer" }} onClick={() => {
              setSearchTerm("");
              setInputValue("");
            }}>
              {t("ES_COMMON_CLEAR_ALL")}
            </p>
          </div>
        </div>

        {geoJsonData.features.length > 0 ? (
          <div ref={mapRef} style={{ height: '86vh', width: '100%', border: '1px solid #ccc', marginTop: "0px" }} />
        ) : (
          <p style={{ textAlign: "center", marginTop: "20px" }}>{t("Loading map data...")}</p>
        )}
      </div>
    </div>
  );
};

export default MapView;
