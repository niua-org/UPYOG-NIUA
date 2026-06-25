import React, { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { BackButton } from "@nudmcdgnpm/digit-ui-react-components";
import { MAP_TILE_URL } from "../utils";
import "../css/gis-inline.css";

// Vite's ?raw suffix loads the GeoJSON file as a plain text string at build time.
// We then JSON.parse() it ourselves.
import hoshiarpurRaw from "../data/hoshiarpur.geojson?raw";
import karolbaghRaw from "../data/karolbagh.geojson?raw";

/**
 * LayerView renders survey parcels as coloured POLYGONS on a Leaflet map.
 *
 * Each dataset is called an "area". Two areas are loaded:
 *   - Hoshiarpur Block 05-A1  → coloured by Property Type (no tax data in that shapefile)
 *   - Karol Bagh (Delhi)      → coloured by real Tax Status (Paid / Due / Overdue / Partial)
 *
 * To add a third area: add one new entry to the AREAS array below.
 * To go live with real backend data: replace each area's `raw` field with an API response.
 */


/**
 * Cleans a single raw field value from the shapefile.
 * Shapefiles often store missing data as blank strings or "-" instead of null.
 * This function converts all of those into a proper null so the popup can hide them.
 */
const cleanVal = (rawValue) => {
  if (rawValue === null || rawValue === undefined) return null;
  const trimmedValue = String(rawValue).trim();
  return trimmedValue === "" || trimmedValue === "-" ? null : trimmedValue;
};

/**
 * AREAS is the master config list. Each entry represents one dataset (one GeoJSON file).
 *
 * Every area defines:
 *   id          — unique identifier used by the dropdown
 *   label       — human-readable name shown in the dropdown
 *   raw         — the imported GeoJSON text (loaded at build time via ?raw)
 *   legendTitle — what the colours represent ("Property Type" / "Tax Status")
 *   colors      — map of category name → hex colour for the polygon fill
 *   filters     — the filter chips shown above the map (ALL + each category)
 *   toFeature   — function that reads one feature's raw shapefile fields and returns
 *                 the common shape: { category, title, rows }
 *                 category = the value we colour + filter by
 *                 title    = heading shown at the top of the popup
 *                 rows     = [[label, value], ...] pairs shown in the popup body
 *
 * QGIS analogy: each entry is like a separate layer in the Layers panel, each with
 * its own symbology (colors), attribute table (rows), and field mapping (toFeature).
 */
const AREAS = [
  {
    id: "hoshiarpur",
    label: "Hoshiarpur · Block 05-A1",
    raw: hoshiarpurRaw,
    legendTitle: "Property Type",
    // One colour per property type. "Unspecified" catches anything not in this list.
    colors: {
      Commercial: "#4e79a7",
      Residential: "#59a14f",
      "Mixed (Residential/Commercial)": "#b07aa1",
      "Public semi public": "#76b7b2",
      Open: "#edc948",
      Unspecified: "#bab0ac",
    },
    // "ALL" always goes first — it shows every parcel regardless of type.
    filters: [
      { code: "ALL", label: "All" },
      { code: "Commercial", label: "Commercial" },
      { code: "Residential", label: "Residential" },
      { code: "Mixed (Residential/Commercial)", label: "Mixed" },
      { code: "Public semi public", label: "Public" },
      { code: "Open", label: "Open" },
      { code: "Unspecified", label: "Other" },
    ],
    // Maps the raw shapefile fields (truncated to 10 chars by dBASE format) → readable rows.
    // Fields like TYPE_OF_PR, OWNER_MOBI, NO_OF_FLOO are truncated shapefile field names.
    toFeature: (rawProperties) => {
      const plotArea = cleanVal(rawProperties.PLOT_AREA);
      return {
        category: cleanVal(rawProperties.TYPE_OF_PR) || "Unspecified", // drives colour + filter
        title: cleanVal(rawProperties.TYPE_OF_PR) || "Property",       // popup heading
        rows: [
          ["Parcel No", cleanVal(rawProperties.Parcel_No)],
          ["Owner", cleanVal(rawProperties.OWNER_NAME)],
          ["Mobile", cleanVal(rawProperties.OWNER_MOBI)],
          ["Block", cleanVal(rawProperties.BLOCK_NO)],
          // Only show unit if the value exists; plotArea && "..." returns null when plotArea is null
          ["Plot Area", plotArea && `${plotArea} sq.ft`],
          ["Floors", cleanVal(rawProperties.NO_OF_FLOO)],
          ["Construction", cleanVal(rawProperties.TYPE_OF_CO)],
        ],
      };
    },
  },
  {
    id: "karolbagh",
    label: "Karol Bagh · Delhi",
    raw: karolbaghRaw,
    legendTitle: "Tax Status",
    // Traffic-light palette: green = paid, yellow = due, orange = partial, red = overdue.
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
    // Karol Bagh fields use a "data_" prefix — completely different schema from Hoshiarpur.
    // toFeature() is the bridge: the rest of the component never touches raw field names.
    toFeature: (rawProperties) => {
      const plotArea = cleanVal(rawProperties.data_Plot_Area_sqm);
      return {
        category: cleanVal(rawProperties.data_Tax_Status) || "Unknown",
        title: cleanVal(rawProperties.data_Property_Type) || "Property",
        rows: [
          ["Property ID", cleanVal(rawProperties.data_Property_ID)],
          ["UPIN", cleanVal(rawProperties.data_UPIN)],
          ["Owner", cleanVal(rawProperties.data_Owner_Name)],
          ["Type", cleanVal(rawProperties.data_Property_Type)],
          ["Floors", cleanVal(rawProperties.data_Floors)],
          ["Plot Area", plotArea && `${plotArea} sqm`],
          ["Tax Status", cleanVal(rawProperties.data_Tax_Status)],
          ["Annual Tax", cleanVal(rawProperties.data_Annual_Tax_Assessed)],
          ["Outstanding", cleanVal(rawProperties.data_Outstanding_Dues)],
          ["Last Payment", cleanVal(rawProperties.data_Last_Payment_Date)],
          ["Ward", cleanVal(rawProperties.data_Ward_No)],
        ],
      };
    },
  },
];

/**
 * Parses the raw GeoJSON string for one area and normalises every feature's
 * properties into the common { category, title, rows } shape.
 * This runs once per area switch (cached by useMemo) — not on every render.
 *
 * QGIS analogy: like running "Field Calculator" on every row of the attribute table
 * to produce a clean set of columns before styling.
 */
const normalizeFeatureCollection = (areaConfig) => ({
  type: "FeatureCollection",
  features: JSON.parse(areaConfig.raw).features.map((rawFeature) => ({
    type: "Feature",
    geometry: rawFeature.geometry,                              // polygon coordinates — unchanged
    properties: areaConfig.toFeature(rawFeature.properties || {}), // normalised attributes
  })),
});

const LayerView = () => {
  const { t } = useTranslation();

  // mapRef  — a reference to the <div> DOM element that Leaflet will draw into.
  // mapObj  — a reference to the Leaflet map instance (window.L.map(...)).
  // layerRef — a reference to the currently visible GeoJSON polygon layer.
  // Refs (useRef) hold values that persist across renders without triggering a re-render.
  const mapRef = useRef(null);
  const mapObj = useRef(null);
  const layerRef = useRef(null);

  // areaId tracks which area the user has selected in the dropdown.
  // filter  tracks which category chip is active ("ALL" means show everything).
  const [areaId, setAreaId] = useState(AREAS[0].id);
  const [filter, setFilter] = useState("ALL");

  // Derive the full area config object from the selected id.
  const area = AREAS.find((a) => a.id === areaId);

  // useMemo caches the parsed + normalised GeoJSON. It only re-runs when areaId changes,
  // not on every render — parsing a 600-feature GeoJSON on every keystroke would be slow.
  const geoJsonData = useMemo(() => normalizeFeatureCollection(area), [areaId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Build a count of how many parcels fall into each category.
  // This drives the numbers shown next to each legend swatch (e.g. "Paid  350").
  const categoryCounts = {};
  geoJsonData.features.forEach((feature) => {
    const category = feature.properties.category;
    categoryCounts[category] = (categoryCounts[category] || 0) + 1;
  });
  const total = geoJsonData.features.length;

  /**
   * Returns the Leaflet style object for one polygon.
   * fillColor comes from the area's color map; falls back to grey if the
   * category is not listed (e.g. a new type added to the data after deploy).
   */
  const getPolygonStyle = (feature) => ({
    color: "#333",       // border colour
    weight: 1,           // border thickness in pixels
    fillColor: area.colors[feature.properties.category] || "#cccccc",
    fillOpacity: 0.75,
  });

  /**
   * Builds the HTML string shown inside the Leaflet popup when a parcel is clicked.
   * Rows where the value is null/empty are filtered out so the popup stays clean.
   */
  const buildPopup = (featureProps) => {
    const rowsHtml = featureProps.rows
      .filter(([, value]) => value !== null && value !== undefined && value !== "")
      .map(([label, value]) => `<b>${t(label)}:</b> ${value}`)
      .join("<br/>");
    return `<div class="gis-popup"><b>${featureProps.title}</b><br/>${rowsHtml}</div>`;
  };

  /**
   * Loads Leaflet from the CDN if it hasn't been loaded yet, then initialises the map.
   * We load it dynamically (via <script> tag) so it's not bundled into the app's JS.
   * script.onload = initializeMap ensures we only call initializeMap AFTER Leaflet
   * has fully downloaded — otherwise window.L would not exist yet and the map would crash.
   * The [] dependency array means this runs exactly once when the component first mounts.
   */
  useEffect(() => {
    const loadLeaflet = () => {
      if (!window.L) {
        // Step 1: add the Leaflet CSS (styles for zoom controls, popups, etc.)
        const link = document.createElement("link");
        link.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
        link.rel = "stylesheet";
        document.head.appendChild(link);

        // Step 2: add the Leaflet JS and start the map only after it finishes loading
        const script = document.createElement("script");
        script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
        script.onload = initializeMap;
        document.head.appendChild(script);
      } else {
        // Leaflet already loaded (e.g. user navigated away and came back) — init directly
        initializeMap();
      }
    };
    loadLeaflet();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * Zooms the map so all visible polygons fit inside the viewport.
   * fitBounds() is Leaflet's equivalent of QGIS "Zoom to Layer".
   * The try/catch handles the edge case where the active filter hides all features
   * (the layer would have an empty bounding box, which throws an error).
   */
  const zoomToLayer = () => {
    const leafletMap = mapObj.current;
    if (leafletMap && layerRef.current) {
      try {
        leafletMap.fitBounds(layerRef.current.getBounds(), { padding: [20, 20] });
      } catch (e) {
        /* layer is empty — no features match the current filter */
      }
    }
  };

  /**
   * Creates the Leaflet map instance, adds the basemap tile layer, then draws parcels.
   * setTimeout(invalidateSize) fixes a common Leaflet bug: if the map container's size
   * hasn't settled yet when Leaflet first renders, tiles appear grey and parcels are
   * offset. Waiting 250ms lets the browser finish laying out the page before recalculating.
   */
  const initializeMap = () => {
    if (!mapRef.current || mapObj.current) return; // already initialised — skip
    const leafletMap = window.L.map(mapRef.current);
    window.L.tileLayer(MAP_TILE_URL, { attribution: "© OpenStreetMap", maxZoom: 20 }).addTo(leafletMap);
    mapObj.current = leafletMap;
    renderPolygonLayer();
    setTimeout(() => {
      leafletMap.invalidateSize(); // recalculate canvas size after layout settles
      zoomToLayer();
    }, 250);
  };

  /**
   * Removes the existing polygon layer (if any) and draws a fresh one.
   * Called whenever the selected area or active filter changes.
   * We always remove-then-redraw rather than updating in place because Leaflet's
   * geoJSON layer has no built-in "replace data" method.
   */
  const renderPolygonLayer = () => {
    const leafletMap = mapObj.current;
    if (!leafletMap || !window.L) return;

    // Remove the old layer so parcels from the previous area/filter don't linger
    if (layerRef.current) {
      leafletMap.removeLayer(layerRef.current);
      layerRef.current = null;
    }

    const polygonLayer = window.L.geoJSON(geoJsonData, {
      style: getPolygonStyle,
      // The filter function runs per-feature: return false to hide a parcel
      filter: (feature) => filter === "ALL" || feature.properties.category === filter,
      // bindPopup attaches a click popup to every polygon
      onEachFeature: (feature, layer) => layer.bindPopup(buildPopup(feature.properties)),
    }).addTo(leafletMap);

    layerRef.current = polygonLayer;
    zoomToLayer(); // auto-zoom so the new area fills the viewport
  };

  // Re-draw the polygon layer whenever the user switches area or clicks a filter chip.
  useEffect(() => {
    renderPolygonLayer();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [areaId, filter]);

  // When the browser window is resized, tell Leaflet to recalculate the map canvas size.
  // Without this, resizing the window leaves grey tiles along the new edges.
  useEffect(() => {
    const onResize = () => mapObj.current && mapObj.current.invalidateSize();
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize); // cleanup on unmount
  }, []);

  return (
    <div className="gis-map-wrapper">
      {/* Leaflet draws into this div. It must be 100% height/width of its parent. */}
      <div ref={mapRef} className="gis-map-container" />

      {/* Back button — overlaid top-right so it doesn't affect page layout */}
      <div className="gis-back-button">
        <BackButton />
      </div>

      {/* ── LEFT INFO PANEL: title + area switcher dropdown + summary counts ── */}
      <div className="gis-panel gis-panel--info">
        <div className="gis-panel__title">{t("Property Map")}</div>
        <div className="gis-panel__subtitle">UPYOG GIS</div>

        <label className="gis-panel__label">{t("Area")}</label>
        {/* Switching area resets the filter to ALL so stale filters don't carry over */}
        <select
          value={areaId}
          onChange={(e) => {
            setAreaId(e.target.value);
            setFilter("ALL");
          }}
          className="gis-panel__select"
        >
          {AREAS.map((areaOption) => (
            <option key={areaOption.id} value={areaOption.id}>
              {areaOption.label}
            </option>
          ))}
        </select>

        {/* Summary row: total parcel count */}
        <div className="gis-summary-row">
          <span>{t("Total Properties")}</span>
          <b>{total}</b>
        </div>
        {/* Summary row: what the colours represent for this area */}
        <div className="gis-summary-row">
          <span>{t("Coloured by")}</span>
          <b className="gis-accent">{t(area.legendTitle)}</b>
        </div>
      </div>

      {/* ── FILTER CHIPS: one button per category + "All" ── */}
      <div className="gis-panel gis-panel--filters">
        <span className="gis-filter__label">{t(area.legendTitle)}:</span>
        {area.filters.map((chip) => (
          <button
            key={chip.code}
            className={`gis-filter-btn${filter === chip.code ? " gis-filter-btn--active" : ""}`}
            onClick={() => setFilter(chip.code)}
          >
            {t(chip.label)}
          </button>
        ))}
      </div>

      {/* ── LEGEND: colour swatch + label + parcel count for each category ── */}
      <div className="gis-panel gis-panel--legend">
        <div className="gis-legend__title">{t(area.legendTitle)}</div>
        {/* Skip the "ALL" chip — it has no colour of its own */}
        {area.filters
          .filter((chip) => chip.code !== "ALL")
          .map((chip) => (
            <div key={chip.code} className="gis-summary-row">
              <span className="gis-legend__label">
                {/* Colour swatch — background stays inline since it's data-driven per category */}
                <span
                  className="gis-legend__swatch"
                  style={{ background: area.colors[chip.code] }}
                />
                {t(chip.label)}
              </span>
              {/* Count from categoryCounts; || 0 handles categories with no matching parcels */}
              <b>{categoryCounts[chip.code] || 0}</b>
            </div>
          ))}
      </div>
    </div>
  );
};

export default LayerView;
