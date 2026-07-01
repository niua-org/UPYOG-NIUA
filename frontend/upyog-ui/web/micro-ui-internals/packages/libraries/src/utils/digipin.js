import React from "react";

// Digipin utility functions - India Post official algorithm
const DIGIPIN_GRID = [
  ['F', 'C', '9', '8'],
  ['J', '3', '2', '7'],
  ['K', '4', '5', '6'],
  ['L', 'M', 'P', 'T']
];

const BOUNDS = {
  minLat: 2.5,
  maxLat: 38.5,
  minLon: 63.5,
  maxLon: 99.5
};

export function getDigiPin(lat, lon) {
  if (lat < BOUNDS.minLat || lat > BOUNDS.maxLat) throw new Error('Latitude out of range');
  if (lon < BOUNDS.minLon || lon > BOUNDS.maxLon) throw new Error('Longitude out of range');

  let minLat = BOUNDS.minLat;
  let maxLat = BOUNDS.maxLat;
  let minLon = BOUNDS.minLon;
  let maxLon = BOUNDS.maxLon;
  let digiPin = '';

  for (let level = 1; level <= 10; level++) {
    const latDiv = (maxLat - minLat) / 4;
    const lonDiv = (maxLon - minLon) / 4;
    let row = 3 - Math.floor((lat - minLat) / latDiv);
    let col = Math.floor((lon - minLon) / lonDiv);
    row = Math.max(0, Math.min(row, 3));
    col = Math.max(0, Math.min(col, 3));
    digiPin += DIGIPIN_GRID[row][col];
    if (level === 3 || level === 6) digiPin += '-';
    maxLat = minLat + latDiv * (4 - row);
    minLat = minLat + latDiv * (3 - row);
    minLon = minLon + lonDiv * col;
    maxLon = minLon + lonDiv;
  }
  return digiPin;
}

// Shared UI to display a generated Digipin along with a link to view it on MapMyIndia.
// `style` lets callers tweak layout (e.g. width / marginBottom) without duplicating the markup.
export function DigipinDisplay({ digipin, style = {} }) {
  if (!digipin) return null;
  return (
    <div
      style={{
        marginTop: "10px",
        padding: "12px 16px",
        backgroundColor: "#f0f0f0",
        borderRadius: "8px",
        border: "1px solid #d4d4d4",
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        ...style,
      }}
    >
      <div>
        <strong>Digipin:</strong> {digipin}
      </div>
      <a
        href={`https://mappls.com/digipin/${digipin}`}
        target="_blank"
        rel="noopener noreferrer"
        style={{ fontSize: "13px", color: "#a82227", textDecoration: "underline", whiteSpace: "nowrap" }}
      >
        View on MapMyIndia
      </a>
    </div>
  );
}