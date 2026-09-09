import React from "react";

const BreakLine = ({ style = {}, className = "" }) => {
  return <hr className={`break-line border-b border-border ${className}`} style={style} />;
};

export default BreakLine;
