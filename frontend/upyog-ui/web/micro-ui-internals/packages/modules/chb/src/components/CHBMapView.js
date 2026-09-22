import React, { useEffect, useRef, useState, useMemo } from 'react';
import { useTranslation } from "react-i18next";
import { CardLabel, SubmitBar, Dropdown, Loader, Modal, CardSubHeader, CardLabelDesc } from '@nudmcdgnpm/digit-ui-react-components';
import "../css/mapview.scss";

const Close = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FFFFFF" width="20px" height="20px">
    <path d="M0 0h24v24H0V0z" fill="none" />
    <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z" />
  </svg>
);

const CloseBtn = (props) => {
  return (
    <div className="icon-bg-secondary chb-close-btn" onClick={props.onClick}>
      <Close />
    </div>
  );
};

const VENUE_TYPE_CONFIGS = {
  COMMUNITY_HALLS: {
    parentMasterType: "CommunityHalls",
    childMasterCode: "HallCode",
    label: "Community Hall",
  },
  PARKS: {
    parentMasterType: "Parks",
    childMasterCode: "ParkCode",
    label: "Park",
  },
  STADIUMS: {
    parentMasterType: "Stadiums",
    childMasterCode: "StadiumCode",
    label: "Stadium",
  },
  GUEST_HOUSES: {
    parentMasterType: "GuestHouses",
    childMasterCode: "GuestHouseCode",
    label: "Guest House",
  },
  CREMATORIUMS: {
    parentMasterType: "Crematoriums",
    childMasterCode: "CrematoriumCode",
    label: "Crematorium",
  },
};

const CHBMapView = () => {
  const mapRef = useRef(null);
  const [userLocation, setUserLocation] = useState(null);
  const [selectedVenueTypeFilter, setSelectedVenueTypeFilter] = useState(null);
  const [selectedVenue, setSelectedVenue] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedVenueForDetails, setSelectedVenueForDetails] = useState(null);
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();

  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  // Fetch Venue Types from MDMS
  const { data: venueTypeList = [] } = Digit.Hooks.useEnabledMDMS(
    tenantId,
    "CHB",
    [{ name: "Venues" }],
    {
      select: (data) => data?.["CHB"]?.["Venues"] || [],
    }
  );

  // Fetch all Venue Masters and Child Codes from MDMS
  const { data: mdmsAllData, isLoading: isVenuesLoading } = Digit.Hooks.useEnabledMDMS(
    tenantId,
    "CHB",
    [
      { name: "CommunityHalls" },
      { name: "Parks" },
      { name: "Stadiums" },
      { name: "GuestHouses" },
      { name: "Crematoriums" },
      { name: "HallCode" },
      { name: "ParkCode" },
      { name: "StadiumCode" },
      { name: "GuestHouseCode" },
      { name: "CrematoriumCode" },
      { name: "CalculationType" },
    ]
  );

  const communityHalls = mdmsAllData?.["CHB"]?.["CommunityHalls"] || [];
  const parks = mdmsAllData?.["CHB"]?.["Parks"] || [];
  const stadiums = mdmsAllData?.["CHB"]?.["Stadiums"] || [];
  const guestHouses = mdmsAllData?.["CHB"]?.["GuestHouses"] || [];
  const crematoriums = mdmsAllData?.["CHB"]?.["Crematoriums"] || [];
  const hallCodes = mdmsAllData?.["CHB"]?.["HallCode"] || [];
  const parkCodes = mdmsAllData?.["CHB"]?.["ParkCode"] || [];
  const stadiumCodes = mdmsAllData?.["CHB"]?.["StadiumCode"] || [];
  const guestHouseCodes = mdmsAllData?.["CHB"]?.["GuestHouseCode"] || [];
  const crematoriumCodes = mdmsAllData?.["CHB"]?.["CrematoriumCode"] || [];
  const calculationTypes = mdmsAllData?.["CHB"]?.["CalculationType"] || [];

  const calculateDistance = (lat1, lng1, lat2, lng2) => {
    const R = 6371;
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLng = ((lng2 - lng1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  const getChildCodes = (venue) => {
    let list = [];
    if (venue.childMasterCode === "HallCode" || venue.venueTypeCode === "COMMUNITY_HALLS") list = hallCodes;
    else if (venue.childMasterCode === "ParkCode" || venue.venueTypeCode === "PARKS") list = parkCodes;
    else if (venue.childMasterCode === "StadiumCode" || venue.venueTypeCode === "STADIUMS") list = stadiumCodes;
    else if (venue.childMasterCode === "GuestHouseCode" || venue.venueTypeCode === "GUEST_HOUSES") list = guestHouseCodes;
    else if (venue.childMasterCode === "CrematoriumCode" || venue.venueTypeCode === "CREMATORIUMS") list = crematoriumCodes;

    return list.filter(
      (c) =>
        c.venueCode === venue.code ||
        (venue.venueId && c.venueId === venue.venueId) ||
        (venue.venueId && c.guestHouseId === venue.venueId)
    );
  };

  const getPriceDisplay = (venue) => {
    const matchingCalc = calculationTypes.find(
      (ct) =>
        ct.communityHallCode === venue.code &&
        (ct.feeType === "BOOKING_FEES" || ct.feeType === "HALL_RENT" || ct.feeType === "RENT")
    );
    if (matchingCalc?.amount) {
      return matchingCalc.calculationDurationType
        ? `₹${matchingCalc.amount} / ${t(matchingCalc.calculationDurationType)}`
        : `₹${matchingCalc.amount}`;
    }
    if (venue.price) {
      return `₹${venue.price}`;
    }
    return t("CS_NA");
  };

  const getCapacityDisplay = (venue) => {
    const codes = getChildCodes(venue);
    const caps = codes.map((c) => c.capacity || c.rooms).filter(Boolean);
    if (caps.length > 0) {
      return `${caps.join(", ")} ${venue.venueTypeCode === "GUEST_HOUSES" ? t("CHB_ROOMS") : t("CHB_PERSONS")}`;
    }
    if (venue.capacity) return `${venue.capacity} ${t("CHB_PERSONS")}`;
    if (venue.rooms) return `${venue.rooms} ${t("CHB_ROOMS")}`;
    return t("CS_NA");
  };

  const parseTerms = (terms) => {
    if (!terms) return [];
    if (Array.isArray(terms)) return terms.map((t) => String(t).trim()).filter(Boolean);
    if (typeof terms === "string") {
      const cleaned = terms.replace(/\\r\\n/g, "\n").replace(/\\n/g, "\n").replace(/\r/g, "");
      return cleaned.split("\n").map((l) => l.trim()).filter(Boolean);
    }
    return [String(terms)];
  };

  const getVenueFacilities = (venue) => {
    const fac = venue.facilities || venue.services || venue.amenities;
    const sports = venue.sportsAvailable;
    const items = [];
    if (Array.isArray(fac)) items.push(...fac);
    else if (typeof fac === "string" && fac.trim()) items.push(fac.trim());

    if (Array.isArray(sports)) items.push(...sports);
    else if (typeof sports === "string" && sports.trim()) items.push(sports.trim());

    return items;
  };

  const getVenueTerms = (venue) => {
    return venue.termsAndCondition || venue.termsAndConditions || venue.tnc || venue.terms_and_conditions || venue.rules || "";
  };

  const getVenueDescription = (venue) => {
    return venue.venueDescription || venue.parkDescription || venue.hallDescription || venue.description || "";
  };

  const getVenueContact = (venue) => {
    return venue.contactDetails || venue.contact_number || venue.contactNumber || venue.phone || "N/A";
  };

  const formatDurationText = (durationStr) => {
    if (!durationStr) return "";
    if (typeof durationStr === "number") {
      return `${durationStr} ${durationStr === 1 ? t("CHB_HOUR") : t("CHB_HOURS")}`;
    }
    const str = String(durationStr).trim();
    if (str === "23:59" || str === "24:00" || str === "24") {
      return `${t("CHB_FULL_DAY")} (24 ${t("CHB_HOURS")})`;
    }
    if (str.includes(":")) {
      const [h, m] = str.split(":").map((v) => parseInt(v, 10) || 0);
      const hourPart = h > 0 ? `${h} ${h === 1 ? t("CHB_HOUR") : t("CHB_HOURS")}` : "";
      const minPart = m > 0 ? `${m} ${t("CHB_MINS")}` : "";
      return [hourPart, minPart].filter(Boolean).join(" ");
    }
    const num = parseInt(str, 10);
    if (!isNaN(num)) {
      return `${num} ${num === 1 ? t("CHB_HOUR") : t("CHB_HOURS")}`;
    }
    return t(str);
  };

  const getSlotDurationDisplay = (venue) => {
    const timeSlots = venue?.timeSlots || venue?.timeSlot;
    if (!timeSlots) return t("CS_NA");

    if (Array.isArray(timeSlots)) {
      if (timeSlots.length === 0) return t("CS_NA");
      return timeSlots
        .map((s) => s.slot || `${s.from || s.fromTime || ""} - ${s.to || s.toTime || ""}`.trim())
        .filter(Boolean)
        .join(", ");
    }

    const min = timeSlots.minDuration;
    const max = timeSlots.maxDuration;

    if (min && max) {
      const minFormatted = formatDurationText(min);
      const maxFormatted = formatDurationText(max);
      if (minFormatted === maxFormatted) return minFormatted;
      return `${t("CHB_MIN")}: ${minFormatted} | ${t("CHB_MAX")}: ${maxFormatted}`;
    }

    if (min) return `${t("CHB_MIN")}: ${formatDurationText(min)}`;
    if (max) return `${t("CHB_MAX")}: ${formatDurationText(max)}`;

    return t("CS_NA");
  };

  // Combine all active venues across all types
  const allVenues = useMemo(() => {
    const list = [];

    const processVenues = (items, venueTypeCode, defaultChildMaster) => {
      (items || []).forEach((item) => {
        if (item.active === false) return;
        const venueTypeMeta = venueTypeList.find((v) => v.code === venueTypeCode) || VENUE_TYPE_CONFIGS[venueTypeCode];
        list.push({
          ...item,
          venueTypeCode: venueTypeCode,
          venueTypeName: venueTypeMeta?.name || VENUE_TYPE_CONFIGS[venueTypeCode]?.label || venueTypeCode,
          parentMasterType: venueTypeMeta?.parentMasterType || VENUE_TYPE_CONFIGS[venueTypeCode]?.parentMasterType,
          childMasterCode: item.childMasterCode || venueTypeMeta?.childMasterCode || defaultChildMaster,
          timeSlots: venueTypeMeta?.timeSlot || { maxDuration: "23:59", minDuration: "1:00" },
        });
      });
    };

    processVenues(communityHalls, "COMMUNITY_HALLS", "HallCode");
    processVenues(parks, "PARKS", "ParkCode");
    processVenues(stadiums, "STADIUMS", "StadiumCode");
    processVenues(guestHouses, "GUEST_HOUSES", "GuestHouseCode");
    processVenues(crematoriums, "CREMATORIUMS", "CrematoriumCode");

    return list;
  }, [communityHalls, parks, stadiums, guestHouses, crematoriums, venueTypeList]);

  // Venue Type dropdown options
  const venueTypeOptions = useMemo(() => {
    const options = [];
    (venueTypeList.length > 0
      ? venueTypeList
      : Object.keys(VENUE_TYPE_CONFIGS).map((k) => ({
          code: k,
          name: VENUE_TYPE_CONFIGS[k].label,
          parentMasterType: VENUE_TYPE_CONFIGS[k].parentMasterType,
        }))
    ).forEach((vt) => {
      if (vt.active !== false) {
        options.push({
          code: vt.code,
          value: vt.code,
          i18nKey: vt.code,
          name: t(vt.code),
          parentMasterType: vt.parentMasterType,
          timeSlots: vt.timeSlot,
        });
      }
    });
    return options;
  }, [venueTypeList, t]);

  // Filtered venues based on selected venue type for map display
  const displayVenuesOnMap = useMemo(() => {
    if (!selectedVenueTypeFilter) {
      return allVenues;
    }
    return allVenues.filter((v) => v.venueTypeCode === selectedVenueTypeFilter.code);
  }, [allVenues, selectedVenueTypeFilter]);

  // Venue dropdown options - ONLY show venue options when venue type is selected, using venue code
  const venueOptions = useMemo(() => {
    if (!selectedVenueTypeFilter) {
      return [];
    }
    return allVenues
      .filter((v) => v.venueTypeCode === selectedVenueTypeFilter.code)
      .map((v) => {
        return {
          code: v.code,
          value: v.code,
          i18nKey: v.code,
          displayName: v.code,
          venueName: v.venueName || v.name || v.code,
          childMasterCode: v.childMasterCode,
          address: v.address,
          contactDetails: getVenueContact(v),
          geoLocation: v.geoLocation,
          parkDescription: v.parkDescription,
          termsAndCondition: getVenueTerms(v),
          venueDescription: getVenueDescription(v),
          venueId: v.venueId,
          venueData: v,
        };
      });
  }, [allVenues, selectedVenueTypeFilter]);

  const handleSearch = () => {
    setSearchTerm(selectedVenue?.code || (typeof selectedVenue === "string" ? selectedVenue : ""));
  };

  const navigateToBooking = (venue) => {
    navigate(`/upyog-ui/citizen/chb/bookHall/searchvenue`, {
      state: {
        selectedVenueType: {
          code: venue.venueTypeCode,
          value: venue.venueTypeName,
          i18nKey: venue.venueTypeCode,
          parentMasterType: venue.parentMasterType,
          timeSlots: venue.timeSlots,
        },
        selectedCommunityHall: {
          code: venue.code,
          value: venue.venueName || venue.name || venue.code,
          i18nKey: venue.code,
          childMasterCode: venue.childMasterCode,
          venueId: venue.venueId,
          communityHallId: venue.venueId || undefined,
          address: venue.address,
          contactDetails: getVenueContact(venue),
          geoLocation: venue.geoLocation,
          venueDescription: getVenueDescription(venue),
          termsAndCondition: getVenueTerms(venue),
        },
      },
    });
  };

  // Expose global methods for Leaflet HTML popups
  useEffect(() => {
    window.showVenueDetails = (venueCode) => {
      const targetVenue = allVenues.find((v) => v.code === venueCode);
      if (targetVenue) {
        setSelectedVenueForDetails(targetVenue);
      }
    };

    window.selectHall = (venueCode, hallId) => {
      const targetVenue = allVenues.find(
        (v) => v.code === venueCode || (hallId && (v.venueId === hallId || v.communityHallId === hallId))
      );
      if (targetVenue) {
        navigateToBooking(targetVenue);
      }
    };

    return () => {
      delete window.showVenueDetails;
      delete window.selectHall;
    };
  }, [allVenues]);

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

  useEffect(() => {
    if (isVenuesLoading) return;

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
  }, [userLocation, searchTerm, displayVenuesOnMap, isVenuesLoading]);

  const initMap = () => {
    if (!mapRef.current || !window.L) return;

    if (mapRef.current._leaflet_id) {
      mapRef.current._leaflet_id = null;
      mapRef.current.innerHTML = "";
    }

    const defaultCenter = userLocation ? [userLocation.lat, userLocation.lng] : [30.683, 76.708];
    const map = window.L.map(mapRef.current).setView(defaultCenter, 12);
    window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

    const boundsPoints = [];

    if (userLocation) {
      const userIcon = window.L.divIcon({
        html: '<div class="user-location-dot">⦿</div>',
        iconSize: [40, 40],
        className: 'user-location-marker',
      });

      window.L.marker([userLocation.lat, userLocation.lng], { icon: userIcon })
        .addTo(map)
        .bindPopup(`<b>${t("CHB_YOUR_LOCATION")}</b>`);

      boundsPoints.push([userLocation.lat, userLocation.lng]);
    }

    const hallIcon = window.L.icon({
      iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
      iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
      shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
      iconSize: [25, 41],
      iconAnchor: [12, 41],
    });

    const markerMap = new Map();

    const parseCoordinates = (venue) => {
      let lat = null;
      let lng = null;

      if (venue.latitude && venue.longitude) {
        lat = parseFloat(venue.latitude);
        lng = parseFloat(venue.longitude);
      } else if (venue.geoLocation && typeof venue.geoLocation === 'string') {
        const coords = venue.geoLocation.split(',').map((s) => parseFloat(s.trim()));
        if (coords.length === 2 && !isNaN(coords[0]) && !isNaN(coords[1])) {
          // Detect if formatted as [lng, lat] (where Longitude > 50 and Latitude < 45 in India) or [lat, lng]
          if (coords[0] > 50 && coords[1] < 45) {
            lng = coords[0];
            lat = coords[1];
          } else {
            lat = coords[0];
            lng = coords[1];
          }
        }
      }

      return { lat, lng };
    };

    displayVenuesOnMap.forEach((venue) => {
      const { lat, lng } = parseCoordinates(venue);

      if (lat === null || lng === null) return;

      boundsPoints.push([lat, lng]);

      const priceDisplay = getPriceDisplay(venue);
      const capacityDisplay = getCapacityDisplay(venue);
      const venueDisplayName = t(venue.code);
      const venueTypeName = t(venue.venueTypeCode);
      const description = getVenueDescription(venue);
      const termsList = parseTerms(getVenueTerms(venue));
      const facilitiesList = getVenueFacilities(venue);
      const imageUrl =
        venue.headerImageUrl ||
        venue.image_url ||
        'https://nugp-assets.s3.ap-south-1.amazonaws.com/nugp+asset/Banner+UPYOG+%281920x500%29B+%282%29.jpg';
      const address = venue.address || '';
      const contactNumber = getVenueContact(venue);
      const distance = userLocation ? calculateDistance(userLocation.lat, userLocation.lng, lat, lng) : null;

      const marker = window.L.marker([lat, lng], {
        icon: hallIcon,
      }).addTo(map);

      const directionUrl = userLocation
        ? `https://www.google.com/maps/dir/${userLocation.lat},${userLocation.lng}/${lat},${lng}`
        : `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;

      // T&C preview for popup
      const tncPreviewHtml =
        termsList.length > 0
          ? `
          <div class="chb-map-popup-tnc-box">
            <div class="chb-map-popup-tnc-header">
              📜 ${t("CHB_TERMS_AND_CONDITIONS")}
            </div>
            <div class="chb-map-popup-tnc-content">
              ${termsList.slice(0, 2).map((item) => `• ${t(item)}`).join("<br>")}
            </div>
          </div>
        `
          : "";

      const facilitiesHtml =
        facilitiesList.length > 0
          ? `
          <div class="chb-map-popup-amenities">
            <b>${t("CHB_FACILITIES_AMENITIES")}:</b> ${facilitiesList.map((f) => t(f)).join(", ")}
          </div>
        `
          : "";

      const popupContent = `
        <div class="chb-map-popup-card">
          <div class="chb-map-popup-top-section">
            <div class="chb-map-popup-header-info">
              <span class="chb-map-popup-badge">
                ${venueTypeName}
              </span>
              <b class="chb-map-popup-title">${venueDisplayName}</b>
              ${address ? `<span class="chb-map-popup-address">📍 ${address}</span>` : ''}
            </div>
            <img src="${imageUrl}" alt="${venueDisplayName}" class="chb-map-popup-image" />
          </div>
          <div class="chb-map-popup-details">
            <div class="chb-map-popup-stat">💰 <b>${t("CHB_PRICE")}:</b> ${priceDisplay}</div>
            <div class="chb-map-popup-stat">👥 <b>${t("CHB_CAPACITY")}:</b> ${capacityDisplay}</div>
            <div class="chb-map-popup-stat">📞 <b>${t("CHB_CONTACT")}:</b> ${contactNumber}</div>
            ${distance !== null ? `<div class="chb-map-popup-stat">🧭 <b>${t("CHB_DISTANCE")}:</b> ${distance.toFixed(1)} km</div>` : ''}
            ${facilitiesHtml}
            ${description ? `<div class="chb-map-popup-desc">${t(description)}</div>` : ''}
          </div>
          ${tncPreviewHtml}
          <div class="chb-map-popup-actions">
            <a href="${directionUrl}" 
              target="_blank" 
              rel="noopener noreferrer"
              class="chb-map-popup-direction-link">
              🗺️ ${t("CHB_GET_DIRECTION")}
            </a>
            <div class="chb-map-popup-btn-group">
              <button
                type="button"
                class="chb-view-details-btn"
                onclick="window.showVenueDetails('${venue.code}')"
                data-venue-code="${venue.code}"
              >
                ${t("CHB_DETAILS_AND_TNC")}
              </button>
              <button
                type="button"
                class="chb-book-now-btn"
                onclick="window.selectHall('${venue.code}')"
                data-venue-code="${venue.code}"
                ${venue.venueId ? `data-venue-id="${venue.venueId}"` : ""}
              >
                ${t("CHB_BOOK_NOW")}
              </button>
            </div>
          </div>
        </div>
      `;

      marker.bindPopup(popupContent, { maxWidth: 360, minWidth: 320 });

      marker.on("popupopen", (e) => {
        const popupElement = e.popup && e.popup.getElement ? e.popup.getElement() : null;
        if (!popupElement) return;

        const detailsBtn = popupElement.querySelector(".chb-view-details-btn");
        if (detailsBtn) {
          detailsBtn.onclick = (event) => {
            event.preventDefault();
            setSelectedVenueForDetails(venue);
          };
        }

        const button = popupElement.querySelector(".chb-book-now-btn");
        if (button) {
          button.onclick = (event) => {
            event.preventDefault();
            navigateToBooking(venue);
          };
        }
      });

      markerMap.set(`${venue.code.toLowerCase()}`, { marker, lat, lng });
      if (venue.venueName) {
        markerMap.set(`${venue.venueName.toLowerCase()}`, { marker, lat, lng });
      }
    });

    // Search and fly-to logic
    if (searchTerm) {
      const term = searchTerm.trim().toLowerCase();
      let matched = false;
      for (let [key, { marker, lat, lng }] of markerMap) {
        if (key.includes(term)) {
          map.flyTo([lat, lng], 16);
          marker.openPopup();
          matched = true;
          break;
        }
      }
      if (!matched && boundsPoints.length > 0) {
        map.fitBounds(boundsPoints, { padding: [50, 50], maxZoom: 14 });
      }
    } else if (boundsPoints.length > 0) {
      map.fitBounds(boundsPoints, { padding: [50, 50], maxZoom: 14 });
    }
  };

  const selectedVenueTermsList = selectedVenueForDetails ? parseTerms(getVenueTerms(selectedVenueForDetails)) : [];
  const selectedVenueFacilities = selectedVenueForDetails ? getVenueFacilities(selectedVenueForDetails) : [];
  const selectedVenueChildCodes = selectedVenueForDetails ? getChildCodes(selectedVenueForDetails) : [];

  return (
    <div>
      <div className="chb-mapview-wrapper">
        <CardLabel className="chb-mapview-search-label">
          {t("CHB_VENUE_SEARCH")}
        </CardLabel>
        <div className="chb-mapview-filter-bar">
          {/* Venue Type Dropdown Filter */}
          <div className="chb-mapview-type-dropdown-container">
            <Dropdown
              className="form-field chb-mapview-type-dropdown"
              selected={selectedVenueTypeFilter}
              select={(val) => {
                setSelectedVenueTypeFilter(val);
                setSelectedVenue(null);
                setSearchTerm("");
              }}
              option={venueTypeOptions}
              placeholder={t("CHB_VENUE_TYPE_PLACEHOLDER")}
              optionKey="name"
              t={t}
            />
          </div>

          {/* Venue Select Dropdown */}
          <div className="chb-mapview-name-dropdown-container">
            <Dropdown
              className="form-field chb-mapview-name-dropdown"
              selected={selectedVenue}
              select={setSelectedVenue}
              option={venueOptions}
              placeholder={t("CHB_VENUE_NAME_PLACEHOLDER")}
              optionKey="i18nKey"
              disable={!selectedVenueTypeFilter || venueOptions.length === 0}
              t={t}
            />
          </div>

          <div className="chb-mapview-action-group">
            <SubmitBar className="chb-mapview-submit-btn" label={t("ES_COMMON_SEARCH")} onSubmit={handleSearch} />
            <p
              className="link chb-mapview-clear-link"
              onClick={() => {
                setSelectedVenueTypeFilter(null);
                setSelectedVenue(null);
                setSearchTerm("");
              }}
            >
              {t("ES_COMMON_CLEAR_ALL")}
            </p>
          </div>
        </div>

        {isVenuesLoading ? (
          <div className="chb-mapview-loader-container">
            <Loader />
          </div>
        ) : (
          <div ref={mapRef} className="chb-mapview-map-container" />
        )}
      </div>

      {/* Modal for Full Venue Details & Terms and Conditions (T&C) */}
      {selectedVenueForDetails && (
        <Modal
          headerBarMain={
            <CardSubHeader className="chb-modal-header-text">
              {t(selectedVenueForDetails.code)}
            </CardSubHeader>
          }
          headerBarEnd={<CloseBtn onClick={() => setSelectedVenueForDetails(null)} />}
          popupStyles={{
            backgroundColor: "#fff",
            position: 'relative',
            width: '90%',
            maxWidth: '1000px',
            maxHeight: '90vh',
            overflowY: 'auto',
            borderRadius: '8px',
          }}
          children={
            <div className="chb-modal-content-body">
              {/* Overview Metric Cards Grid */}
              <div className="chb-modal-overview-grid">
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_HALL_CODE_LABEL")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-code-value">
                    {t(selectedVenueForDetails.code)}
                  </CardLabelDesc>
                </div>
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_VENUE_TYPE_LABEL")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value-bold">
                    {t(selectedVenueForDetails.venueTypeCode)}
                  </CardLabelDesc>
                </div>
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_BOOKING_PRICE")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value-bold">
                    {getPriceDisplay(selectedVenueForDetails)}
                  </CardLabelDesc>
                </div>
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_TOTAL_CAPACITY")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value-bold">
                    {getCapacityDisplay(selectedVenueForDetails)}
                  </CardLabelDesc>
                </div>
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_CONTACT_DETAILS")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value-bold">
                    {t(getVenueContact(selectedVenueForDetails))}
                  </CardLabelDesc>
                </div>
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_GEO_LOCATION")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value">
                    {selectedVenueForDetails.geoLocation || t("CS_NA")}
                  </CardLabelDesc>
                </div>
                <div>
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_BOOKING_DURATION")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value">
                    {getSlotDurationDisplay(selectedVenueForDetails)}
                  </CardLabelDesc>
                </div>
                <div className="chb-modal-grid-span2">
                  <CardLabel className="chb-modal-grid-label">
                    {t("CHB_ADDRESS")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-grid-value">
                    {selectedVenueForDetails.address || t("CS_NA")}
                  </CardLabelDesc>
                </div>
              </div>

              {/* Sub-Units Breakdown (if child codes exist) */}
              {selectedVenueChildCodes.length > 0 && (
                <div className="chb-modal-section-container">
                  <CardLabel className="chb-modal-section-heading">
                    {t("CHB_AVAILABLE_UNITS")} ({selectedVenueChildCodes.length})
                  </CardLabel>
                  <div className="chb-modal-units-flex">
                    {selectedVenueChildCodes.map((child, idx) => (
                      <div key={idx} className="chb-modal-unit-card">
                        <div className="chb-modal-unit-card-title">
                          {t(child.code)}
                        </div>
                        {(child.capacity || child.rooms) && (
                          <div className="chb-modal-unit-card-capacity">
                            {t("CHB_CAPACITY")}: <b>{child.capacity || child.rooms}</b> {selectedVenueForDetails.venueTypeCode === "GUEST_HOUSES" ? t("CHB_ROOMS") : t("CHB_PERSONS")}
                          </div>
                        )}
                        {child.rent && (
                          <div className="chb-modal-unit-card-rent">
                            {t("CHB_RENT")}: ₹{child.rent}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Description */}
              {getVenueDescription(selectedVenueForDetails) && (
                <div className="chb-modal-section-container">
                  <CardLabel className="chb-modal-section-heading">
                    {t("CHB_DESCRIPTION")}
                  </CardLabel>
                  <CardLabelDesc className="chb-modal-desc-content">
                    {t(getVenueDescription(selectedVenueForDetails))}
                  </CardLabelDesc>
                </div>
              )}

              {/* Facilities & Services */}
              {selectedVenueFacilities.length > 0 && (
                <div className="chb-modal-section-container">
                  <CardLabel className="chb-modal-section-heading">
                    {t("CHB_FACILITIES_AMENITIES")}
                  </CardLabel>
                  <div className="chb-modal-amenities-flex">
                    {selectedVenueFacilities.map((fac, idx) => (
                      <span key={idx} className="chb-modal-amenity-chip">
                        ✓ {t(fac)}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Terms and Conditions (T&C) */}
              <div className="chb-modal-tnc-container">
                <CardLabel className="chb-modal-section-heading">
                  📜 {t("CHB_TERMS_AND_CONDITIONS")}
                </CardLabel>
                <div className="chb-modal-tnc-scrollbox">
                  {selectedVenueTermsList.length > 0 ? (
                    <ol className="chb-modal-tnc-ol">
                      {selectedVenueTermsList.map((line, idx) => (
                        <li key={idx} className="chb-modal-tnc-li">
                          {t(line)}
                        </li>
                      ))}
                    </ol>
                  ) : (
                    <p className="chb-modal-tnc-empty-msg">
                      {t("NO_TERMS_AVAILABLE")}
                    </p>
                  )}
                </div>
              </div>

              {/* Disclaimer / Remarks if present */}
              {(selectedVenueForDetails.disclaimer || selectedVenueForDetails.remarks) && (
                <div className="chb-modal-remarks-container">
                  <div className="chb-modal-remarks-heading">
                    {t("CHB_REMARKS")}
                  </div>
                  <div className="chb-modal-remarks-text">
                    {t(selectedVenueForDetails.disclaimer || selectedVenueForDetails.remarks)}
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              <div className="chb-modal-footer-action-bar">
                <SubmitBar
                  label={t("CS_COMMON_CLOSE")}
                  onSubmit={() => setSelectedVenueForDetails(null)}
                />
                <SubmitBar
                  label={t("CHB_BOOK_NOW")}
                  onSubmit={() => {
                    const venueToBook = selectedVenueForDetails;
                    setSelectedVenueForDetails(null);
                    navigateToBooking(venueToBook);
                  }}
                />
              </div>
            </div>
          }
          actionCancelLabel={null}
          actionSaveLabel={null}
          hideSubmit={true}
          popupModuleMianStyles={{ padding: "0" }}
          headerBarMainStyle={{ position: "sticky", top: 0, backgroundColor: "#f5f5f5", zIndex: 10 }}
          popupModuleActionBarStyles={{ display: 'none' }}
          isOpen={!!selectedVenueForDetails}
          onClose={() => setSelectedVenueForDetails(null)}
        />
      )}
    </div>
  );
};

export default CHBMapView;
