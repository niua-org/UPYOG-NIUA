import React, { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { BackButton } from "@nudmcdgnpm/digit-ui-react-components";
import { MAP_TILE_URL } from "../utils";
// Vite's ?raw imports each GeoJSON file's text directly — no separate generated .js needed.
import block05Raw from "../data/block05.geojson?raw";
import karolbaghRaw from "../data/karolbagh.geojson?raw";

/**
 * PropertyTaxMap renders survey parcels as POLYGONS for one selected AREA.
 * Each area has its own GeoJSON, its own field names, and what it is coloured by:
 *   - Hoshiarpur Block 05-A1  → coloured by Property Type (no tax data in that survey)
 *   - Karol Bagh (Delhi)      → coloured by real Tax Status (Paid/Due/Overdue/Partial)
 *
 * Adding another area = add one entry to the AREAS list below. To go live, replace each
 * area's `raw` GeoJSON with a Digit.GIS.searchPT(polygon) response.
 */

// Height of the UPYOG top header. The map is pinned below it and fills the rest of the
// viewport edge-to-edge. If a thin gap/overlap shows under the header, nudge this number.
const HEADER_OFFSET = 56;

// Tidy a single survey value: blanks and "-" become null.
const cleanVal = (v) => {
  if (v === null || v === undefined) return null;
  const s = String(v).trim();
  return s === "" || s === "-" ? null : s;
};

/**
 * One config per area. `toFeature` maps that area's raw fields into a common shape:
 *   - category : the value we colour by (and filter on)
 *   - title    : popup heading
 *   - rows     : [label, value] pairs shown in the popup (nulls are hidden)
 */
const AREAS = [
  {
    id: "hoshiarpur",
    label: "Hoshiarpur · Block 05-A1",
    raw: block05Raw,
    legendTitle: "Property Type",
    colors: {
      Commercial: "#4e79a7",
      Residential: "#59a14f",
      "Mixed (Residential/Commercial)": "#b07aa1",
      "Public semi public": "#76b7b2",
      Open: "#edc948",
      Unspecified: "#bab0ac",
    },
    filters: [
      { code: "ALL", label: "All" },
      { code: "Commercial", label: "Commercial" },
      { code: "Residential", label: "Residential" },
      { code: "Mixed (Residential/Commercial)", label: "Mixed" },
      { code: "Public semi public", label: "Public" },
      { code: "Open", label: "Open" },
      { code: "Unspecified", label: "Other" },
    ],
    toFeature: (p) => {
      const area = cleanVal(p.PLOT_AREA);
      return {
        category: cleanVal(p.TYPE_OF_PR) || "Unspecified",
        title: cleanVal(p.TYPE_OF_PR) || "Property",
        rows: [
          ["Parcel No", cleanVal(p.Parcel_No)],
          ["Owner", cleanVal(p.OWNER_NAME)],
          ["Mobile", cleanVal(p.OWNER_MOBI)],
          ["Block", cleanVal(p.BLOCK_NO)],
          ["Plot Area", area && `${area} sq.ft`],
          ["Floors", cleanVal(p.NO_OF_FLOO)],
          ["Construction", cleanVal(p.TYPE_OF_CO)],
        ],
      };
    },
  },
  {
    id: "karolbagh",
    label: "Karol Bagh · Delhi",
    raw: karolbaghRaw,
    legendTitle: "Tax Status",
    colors: {
      Paid: "#59a14f",
      Due: "#edc948",
      "Partially Paid": "#f28e2b",
      Overdue: "#e15759",
      Unknown: "#bab0ac",
    },
    filters: [
      { code: "ALL", label: "All" },
      { code: "Paid", label: "Paid" },
      { code: "Due", label: "Due" },
      { code: "Partially Paid", label: "Partial" },
      { code: "Overdue", label: "Overdue" },
    ],
    toFeature: (p) => {
      const plot = cleanVal(p.data_Plot_Area_sqm);
      return {
        category: cleanVal(p.data_Tax_Status) || "Unknown",
        title: cleanVal(p.data_Property_Type) || "Property",
        rows: [
          ["Property ID", cleanVal(p.data_Property_ID)],
          ["UPIN", cleanVal(p.data_UPIN)],
          ["Owner", cleanVal(p.data_Owner_Name)],
          ["Type", cleanVal(p.data_Property_Type)],
          ["Floors", cleanVal(p.data_Floors)],
          ["Plot Area", plot && `${plot} sqm`],
          ["Tax Status", cleanVal(p.data_Tax_Status)],
          ["Annual Tax", cleanVal(p.data_Annual_Tax_Assessed)],
          ["Outstanding", cleanVal(p.data_Outstanding_Dues)],
          ["Last Payment", cleanVal(p.data_Last_Payment_Date)],
          ["Ward", cleanVal(p.data_Ward_No)],
        ],
      };
    },
  },
];

// Build a normalised FeatureCollection for one area (parse + map fields once).
const buildFeatures = (area) => ({
  type: "FeatureCollection",
  features: JSON.parse(area.raw).features.map((f) => ({
    type: "Feature",
    geometry: f.geometry,
    properties: area.toFeature(f.properties || {}),
  })),
});

const PropertyTaxMap = () => {
  const { t } = useTranslation();
  const mapRef = useRef(null);
  const mapObj = useRef(null);
  const layerRef = useRef(null);
  const [areaId, setAreaId] = useState(AREAS[0].id);
  const [filter, setFilter] = useState("ALL");

  const area = AREAS.find((a) => a.id === areaId);
  // Parse + normalise only the selected area's data; recompute when the area changes.
  const geoJsonData = useMemo(() => buildFeatures(area), [areaId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Count parcels per category for the legend.
  const counts = {};
  geoJsonData.features.forEach((f) => {
    const k = f.properties.category;
    counts[k] = (counts[k] || 0) + 1;
  });
  const total = geoJsonData.features.length;

  const styleFor = (feature) => ({
    color: "#333",
    weight: 1,
    fillColor: area.colors[feature.properties.category] || "#cccccc",
    fillOpacity: 0.75,
  });

  const popupFor = (props) => {
    const rows = props.rows
      .filter(([, v]) => v !== null && v !== undefined && v !== "")
      .map(([k, v]) => `<b>${t(k)}:</b> ${v}`)
      .join("<br/>");
    return `<div style="font-size:13px;line-height:1.5;min-width:230px">
      <b>${props.title}</b><br/>${rows}</div>`;
  };

  // Load Leaflet (same dynamic-load pattern as MapView) then init the map once.
  useEffect(() => {
    const loadLeaflet = () => {
      if (!window.L) {
        const link = document.createElement("link");
        link.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
        link.rel = "stylesheet";
        document.head.appendChild(link);

        const script = document.createElement("script");
        script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
        script.onload = initMap;
        document.head.appendChild(script);
      } else {
        initMap();
      }
    };
    loadLeaflet();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fitToData = () => {
    const map = mapObj.current;
    if (map && layerRef.current) {
      try {
        map.fitBounds(layerRef.current.getBounds(), { padding: [20, 20] });
      } catch (e) {
        /* no features */
      }
    }
  };

  const initMap = () => {
    if (!mapRef.current || mapObj.current) return;
    const map = window.L.map(mapRef.current);
    window.L.tileLayer(MAP_TILE_URL, { attribution: "© OpenStreetMap", maxZoom: 20 }).addTo(map);
    mapObj.current = map;
    drawLayer();
    // Leaflet must know the container's final size before it can place tiles and centre
    // correctly; recalc once the layout settles to avoid the grey offset.
    setTimeout(() => {
      map.invalidateSize();
      fitToData();
    }, 250);
  };

  // (Re)draw the polygon layer whenever the area or filter changes.
  const drawLayer = () => {
    const map = mapObj.current;
    if (!map || !window.L) return;
    if (layerRef.current) {
      map.removeLayer(layerRef.current);
      layerRef.current = null;
    }
    const layer = window.L.geoJSON(geoJsonData, {
      style: styleFor,
      filter: (f) => filter === "ALL" || f.properties.category === filter,
      onEachFeature: (f, lyr) => lyr.bindPopup(popupFor(f.properties)),
    }).addTo(map);
    layerRef.current = layer;
    fitToData();
  };

  useEffect(() => {
    drawLayer();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [areaId, filter]);

  // Keep the map correctly sized when the window/panel changes.
  useEffect(() => {
    const onResize = () => mapObj.current && mapObj.current.invalidateSize();
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  const panel = {
    position: "absolute",
    zIndex: 1000,
    background: "#fff",
    borderRadius: "10px",
    boxShadow: "0 4px 18px rgba(0,0,0,.18)",
    padding: "12px 16px",
  };
  const fbtn = (active) => ({
    border: active ? "1px solid #2b2350" : "1px solid #ddd",
    background: active ? "#2b2350" : "#fafafa",
    color: active ? "#fff" : "#444",
    borderRadius: "16px",
    padding: "5px 12px",
    fontSize: "12px",
    cursor: "pointer",
    marginRight: "6px",
    marginTop: "2px",
  });

  return (
    <div
      style={{
        position: "fixed",
        top: HEADER_OFFSET,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 1,
        overflow: "hidden",
      }}
    >
      <div ref={mapRef} style={{ height: "100%", width: "100%" }} />

      {/* Back button overlaid so it doesn't take layout space */}
      <div style={{ position: "absolute", top: 40, right: 12, zIndex: 1100 }}>
        <BackButton />
      </div>

      {/* Summary + area switcher */}
      <div style={{ ...panel, top: 40, left: 60, width: 230 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: "#2b2350" }}>{t("Property Map")}</div>
        <div style={{ fontSize: 11, color: "#888", marginBottom: 8 }}>UPYOG GIS</div>
        <label style={{ fontSize: 11, color: "#888" }}>{t("Area")}</label>
        <select
          value={areaId}
          onChange={(e) => {
            setAreaId(e.target.value);
            setFilter("ALL");
          }}
          style={{ width: "100%", padding: "6px", margin: "4px 0 10px", borderRadius: 6, border: "1px solid #ccc", fontSize: 13 }}
        >
          {AREAS.map((a) => (
            <option key={a.id} value={a.id}>
              {a.label}
            </option>
          ))}
        </select>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, padding: "3px 0" }}>
          <span>{t("Total Properties")}</span>
          <b>{total}</b>
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, padding: "3px 0" }}>
          <span>{t("Coloured by")}</span>
          <b style={{ color: "#2b2350" }}>{t(area.legendTitle)}</b>
        </div>
      </div>

      {/* Filters */}
      <div style={{ ...panel, top: 40, left: 306, display: "flex", alignItems: "center", flexWrap: "wrap", maxWidth: "55vw" }}>
        <span style={{ fontSize: 11, color: "#888", marginRight: 6 }}>{t(area.legendTitle)}:</span>
        {area.filters.map((s) => (
          <button key={s.code} style={fbtn(filter === s.code)} onClick={() => setFilter(s.code)}>
            {t(s.label)}
          </button>
        ))}
      </div>

      {/* Legend */}
      <div style={{ ...panel, bottom: 20, left: 60, width: 230 }}>
        <div style={{ fontSize: 11, letterSpacing: ".5px", color: "#888", textTransform: "uppercase", marginBottom: 8 }}>
          {t(area.legendTitle)}
        </div>
        {area.filters
          .filter((s) => s.code !== "ALL")
          .map((s) => (
            <div key={s.code} style={{ display: "flex", justifyContent: "space-between", fontSize: 13, padding: "3px 0" }}>
              <span style={{ display: "flex", alignItems: "center" }}>
                <span
                  style={{
                    width: 14,
                    height: 14,
                    borderRadius: 3,
                    background: area.colors[s.code],
                    border: "1px solid rgba(0,0,0,.25)",
                    marginRight: 8,
                  }}
                />
                {t(s.label)}
              </span>
              <b>{counts[s.code] || 0}</b>
            </div>
          ))}
      </div>
    </div>
  );
};

export default PropertyTaxMap;
