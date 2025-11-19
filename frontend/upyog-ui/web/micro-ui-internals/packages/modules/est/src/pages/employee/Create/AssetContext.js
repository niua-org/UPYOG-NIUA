import React, { useState } from "react";
import React, { createContext, useState, useContext } from "react";

const AssetContext = createContext();

export const AssetProvider = ({ children }) => {
  const [assetData, setAssetData] = useState(null);
  return (
    <AssetContext.Provider value={{ assetData, setAssetData }}>
      {children}
    </AssetContext.Provider>
  );
};

export const useAsset = () => useContext(AssetContext);
