// All inline styles for the ProfileDetails page
export const profileStyles = {

  documentUpload: {
    grid: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
      gap: 20,
      width: "100%",
    },
    card: {
      display: "flex",
      flexDirection: "column",
      gap: 16,
      padding: 20,
      border: "1px solid #E8EAED",
      borderRadius: 12,
      background: "#FFFFFF",
    },
    header: { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 },
    title: { fontWeight: 600, fontSize: 16, color: "#0B0C0C" },

    badge: { fontSize: 12, fontWeight: 600, padding: "4px 10px", borderRadius: 999, whiteSpace: "nowrap" },
    badgeVerified: { background: "#F0FDF4", color: "#166534", border: "1px solid #BBF7D0" },
    badgePending: { background: "#FFF7ED", color: "#9A3412", border: "1px solid #FED7AA" },

    // uploaded file tile: icon, file name below it, meta, view link
    fileTile: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 8,
      width: "100%",
      padding: "20px 16px",
      border: "1px solid #E8EAED",
      borderRadius: 10,
      background: "#FAFAFB",
      cursor: "pointer",
      font: "inherit",
    },
    fileEmoji: { fontSize: 40, lineHeight: 1 },
    fileName: {
      display: "block",
      maxWidth: "100%",
      fontSize: 14,
      fontWeight: 600,
      color: "#0B0C0C",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap",
    },
    fileMeta: { fontSize: 12, color: "#6B7280" },
    viewLink: { fontSize: 13, color: "#1D70B8", textDecoration: "underline" },

    emptyTile: {
      padding: "28px 16px",
      border: "1px dashed #C9CDD2",
      borderRadius: 10,
      background: "#FAFAFB",
      color: "#6B7280",
      fontSize: 14,
      textAlign: "center",
    },

    uploadLabel: { fontSize: 14, fontWeight: 500, color: "#505A5F", marginBottom: 6 },
    hint: { fontSize: 12, color: "#6B7280", marginTop: 6 },
  },
  label: { 
    display: "block", 
    fontSize: 14, 
    color: "#505A5F", 
    marginBottom: 6
  },

  required: { 
    color: "#D4351C" 
  },

  actions: { 
    display: "flex", 
    justifyContent: "flex-end", 
    marginTop: 8 
  },

  documentUploadCard: {
    wrapper: {
      display: "flex",
      flexDirection: "column",
      width: "100%",
    },
    dropdownMargin: {
      width: "100%",
      marginBottom: "8px",
    },
    fileBox: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      border: "1px solid #D6D5D5",
      borderRadius: "6px",
      padding: "6px 10px",
      background: "#FFFFFF",
      minHeight: "44px",
      boxSizing: "border-box",
    },
    fileBoxError: {
      border: "1px solid #D4351C",
    },
    fileBoxDisabled: {
      background: "#F4F5F7",
    },
    fileInner: {
      display: "flex",
      alignItems: "center",
      gap: "10px",
      overflow: "hidden",
      flex: 1,
    },
    chooseBtn: {
      background: "#D1D1D6",
      color: "#0B0C0C",
      border: "none",
      borderRadius: "4px",
      padding: "6px 14px",
      fontSize: "13px",
      fontWeight: 500,
      cursor: "pointer",
      whiteSpace: "nowrap",
      flexShrink: 0,
    },
    chooseBtnDisabled: {
      background: "#E0E0E0",
      cursor: "not-allowed",
    },
    fileName: {
      fontSize: "13px",
      color: "#7C7E80",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap",
    },
    fileNameUploaded: {
      color: "#0B0C0C",
    },
    deleteBtn: {
      background: "transparent",
      border: "none",
      cursor: "pointer",
      padding: "4px 8px",
      color: "#505A5F",
      fontSize: "16px",
      fontWeight: "bold",
      lineHeight: 1,
    },
    errorMargin: {
      marginTop: "4px",
    },
    fileRestrictionText: {
      fontSize: "12px",
      color: "#64748b",
      marginTop: "4px",
      lineHeight: "1.3",
    }
  },

  addressPopUp: { width: "min(900px, 95vw)",
    maxWidth: "900px",
    maxHeight: "90vh",
    overflowY: "auto",
    padding: "24px",
    borderRadius: "12px",
    border: "1px solid #e8eaed",
    boxShadow: "0 8px 24px rgba(0, 0, 0, 0.16)",
    boxSizing: "border-box",
    backgroundColor: "#ffffff"
  }
};