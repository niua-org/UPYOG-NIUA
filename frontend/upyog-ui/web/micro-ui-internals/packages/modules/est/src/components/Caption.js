import React from "react";

const Caption = ({ data, OpenImage }) => {
  const { date, name, mobileNumber, comment, wfComment, source, thumbnailsToShow } = data;

  return (
    <div style={{ padding: "8px 0" }}>
      {date && (
        <div style={{ fontSize: "12px", color: "#666", marginBottom: "4px" }}>
          {new Date(date).toLocaleDateString("en-GB")} {new Date(date).toLocaleTimeString("en-GB")}
        </div>
      )}
      
      {name && (
        <div style={{ fontSize: "14px", fontWeight: "500", marginBottom: "2px" }}>
          {name} {mobileNumber && `(${mobileNumber})`}
        </div>
      )}
      
      {source && (
        <div style={{ fontSize: "12px", color: "#666", marginBottom: "4px" }}>
          Source: {source}
        </div>
      )}
      
      {(comment || wfComment) && (
        <div style={{ fontSize: "13px", color: "#333", marginTop: "4px" }}>
          {wfComment || comment}
        </div>
      )}
      
      {thumbnailsToShow && (
        <div 
          style={{ fontSize: "12px", color: "#007bff", cursor: "pointer", marginTop: "4px" }}
          onClick={() => OpenImage && OpenImage(thumbnailsToShow)}
        >
          View Attachment
        </div>
      )}
    </div>
  );
};

export default Caption;