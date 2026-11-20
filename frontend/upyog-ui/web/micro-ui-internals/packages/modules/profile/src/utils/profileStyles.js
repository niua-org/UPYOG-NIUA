import { pad } from "lodash";

// All inline styles for the ProfileDetails page
export const profileStyles = {
  message: { padding: 16 },

  // Page header
  header: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 },
  headerLeft: { display: "flex", alignItems: "center", gap: 12 },
  headerIcon: {
    width: 48,
    height: 48,
    borderRadius: 8,
    background: "#F4F5F7",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: 22,
  },
  title: { margin: 0, fontSize: 24, fontWeight: 600 },
  verifiedBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    padding: "4px 10px",
    borderRadius: 16,
    backgroundColor: "#E6F4EA",
    border: "1px solid #CEEAD6",
    color: "#137333",
    fontSize: "13px",
    fontWeight: 600,
    marginLeft: 8,
  },
  verifiedIconWrap: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 18,
    height: 18,
    borderRadius: "50%",
    backgroundColor: "#00703C",
    flexShrink: 0,
  },
  unverifiedBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    padding: "4px 10px",
    borderRadius: 16,
    backgroundColor: "#FCE8E6",
    border: "1px solid #FAD2CF",
    color: "#C5221F",
    fontSize: "13px",
    fontWeight: 600,
    marginLeft: 8,
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  unverifiedIconWrap: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 18,
    height: 18,
    borderRadius: "50%",
    backgroundColor: "#D4351C",
    fill: "#FFFFFF",
    color: "#FFFFFF",
    flexShrink: 0,
    overflow: "hidden",
  },
  closeSvgWrap: {
    display: "inline-flex",
    transform: "scale(0.55)",
  },
  unverifiedText: {
    textDecoration: "underline",
  },
  editButton: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    background: "#F4F5F7",
    color: "#0B0C0C",
    border: "none",
    borderRadius: 8,
    padding: "12px 20px",
    fontSize: 16,
    fontWeight: 500,
    cursor: "pointer",
  },
  actionButton: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    background: "#F4F5F7",
    color: "#0B0C0C",
    border: "none",
    borderRadius: 8,
    padding: "12px 20px",
    fontSize: 16,
    fontWeight: 500,
    cursor: "pointer",
  },
  actionIconAdd: { width: 14, height: 14 },
  actionIconPerson: { fill: "#a82227", width: 20, height: 20 },

  // Section + card
  section: { marginBottom: 24 },
  sectionTitle: { margin: "0 0 12px", fontSize: 18, fontWeight: 500 },
  card: {
    display: "flex",
    flexWrap: "wrap",
    gap: 24,
    padding: 20,
    border: "1px solid #E8EAED",
    borderRadius: 12,
    background: "#FCFCFD",
    marginBottom: 12,
  },
  
  cardLabel: {
    width: "100%",
    fontWeight: 700,
    color: "#0B0C0C",
    fontSize: "15px",
    paddingBottom: "10px",
    borderBottom: "2px solid #E8EAED",
    marginBottom: "4px",
  },

  // Photo and its fallback
  photo: { width: 160, height: 176, borderRadius: 12, objectFit: "cover", background: "#F4F5F7" },
  photoPlaceholder: {
    width: 160,
    height: 176,
    borderRadius: 12,
    background: "#E8EAED",
    color: "#505A5F",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: 48,
    fontWeight: 600,
  },

  // Fields grid (wraps automatically on small screens)
  grid: {
    flex: 1,
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
    gap: "20px 24px",
    alignContent: "start",
  },
  fieldLabel: { fontSize: 14, color: "#505A5F", marginBottom: 4 },
  fieldValue: { fontSize: 18, fontWeight: 500, wordBreak: "break-word" },

  // Avatar with camera button
  avatarWrap: { position: "relative", width: 160, height: 176 },
  cameraButton: {
    position: "absolute",
    right: 8,
    bottom: 8,
    width: 36,
    height: 36,
    borderRadius: "50%",
    border: "none",
    background: "#F47738",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
  },

  // Profile Photo Upload Card (CreateProfile)
  photoUploadCard: {
    display: "flex",
    alignItems: "center",
    gap: "24px",
    padding: "16px 20px",
    marginBottom: "24px",
    background: "#F8F9FA",
    borderRadius: "10px",
    border: "1px solid #E8EAED",
    width: "100%",
    boxSizing: "border-box",
  },
  photoUploadCardError: {
    display: "flex",
    alignItems: "center",
    gap: "24px",
    padding: "16px 20px",
    marginBottom: "24px",
    background: "#F8F9FA",
    borderRadius: "10px",
    border: "1px solid #D4351C",
    width: "100%",
    boxSizing: "border-box",
  },
  photoDetailsWrap: {
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  photoTitle: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 600,
    color: "#0B0C0C",
  },
  photoHelpText: {
    fontSize: "14px",
    color: "#505A5F",
  },
  requiredAsterisk: {
    color: "#D4351C",
    fontWeight: 600,
    marginLeft: "4px",
  },
  photoErrorMessage: {
    color: "#D4351C",
    fontSize: "12px",
    fontWeight: 500,
    marginTop: "2px",
  },
  photoButtonRow: {
    display: "flex",
    gap: "10px",
    marginTop: "4px",
  },
  photoUploadBtn: {
    background: "#FFFFFF",
    border: "1px solid #D6D5D5",
    borderRadius: "6px",
    padding: "6px 14px",
    fontSize: "13px",
    fontWeight: 500,
    color: "#0B0C0C",
    cursor: "pointer",
  },
  photoUploadBtnError: {
    background: "#FFFFFF",
    border: "1px solid #D4351C",
    borderRadius: "6px",
    padding: "6px 14px",
    fontSize: "13px",
    fontWeight: 500,
    color: "#D4351C",
    cursor: "pointer",
  },
  photoRemoveBtn: {
    background: "#FFFFFF",
    border: "1px solid #FAD2CF",
    borderRadius: "6px",
    padding: "6px 14px",
    fontSize: "13px",
    fontWeight: 500,
    color: "#D4351C",
    cursor: "pointer",
  },

  // Form
  label: { display: "block", fontSize: 14, color: "#505A5F", marginBottom: 6 },
  required: { color: "#D4351C" },
  actions: { display: "flex", justifyContent: "flex-end", marginTop: 8 },
  primaryButton: {
    margin: 0,
    background: "var(--app-btn-primary-gradient, linear-gradient(90deg, #6A4A91 0%, #3E285F 100%))",
    padding: "0 28px",
    height: 44,
    color: "#ffffff",
    borderRadius: 22,
    border: "none",
    fontSize: 14,
    fontWeight: 600,
    cursor: "pointer",
    flexShrink: 0,
    transition: "all 0.2s ease",
  },
  primaryButtonDisabled: { opacity: 0.6, cursor: "not-allowed" },

  // Tabs (citizen)
  tabs: { display: "flex", gap: 8, marginBottom: 16 },
  tab: {
    margin: 0,
    background: "#D6D6D6",
    padding: "0 28px",
    height: 44,
    color: "#0B0C0C",
    borderRadius: 22,
    border: "none",
    fontSize: 14,
    fontWeight: 600,
    cursor: "pointer",
    flexShrink: 0,
    transition: "all 0.2s ease",
  },
  tabActive: {
    background: "var(--app-btn-primary-gradient, linear-gradient(90deg, #6A4A91 0%, #3E285F 100%))",
    color: "#ffffff",
  },

  // Address tab
  addressActions: { display: "flex", justifyContent: "flex-end", marginBottom: 12 },
  cardHeader: { width: "100%", display: "flex", justifyContent: "space-between", alignItems: "center" },
  documentGrid: {
    display: "flex",
    gap: 24,
    flexWrap: "wrap",
    width: "100%",
  },
  documentCard: {
    display: "flex",
    alignItems: "center",
    gap: 12,
    padding: "12px 16px",
    border: "1px solid #E8EAED",
    borderRadius: 8,
    background: "#FFFFFF",
    minWidth: 240,
    cursor: "pointer",
  },
  documentContent: {
    flex: 1,
  },
  documentTitle: {
    fontWeight: 600,
    fontSize: 14,
    color: "#0B0C0C",
  },
  documentLink: {
    fontSize: 13,
    color: "#1D70B8",
    textDecoration: "underline",
    cursor: "pointer",
    display: "inline-block",
    marginTop: 4,
  },

  headerActions: {
    display: "flex",
    alignItems: "center",
    gap: 8,
  },
  familyIdBadge: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    background: "#F4F5F7",
    color: "#0B0C0C",
    borderRadius: 8,
    padding: "12px 18px",
    fontSize: 15,
    fontWeight: 500,
    border: "1px solid #E8EAED",
  },
  familyIdLabel: {
    fontSize: "14px",
    color: "#505A5F",
    fontWeight: 500,
  },
  familyIdValue: {
    fontSize: "15px",
    color: "#0B0C0C",
    fontWeight: 700,
  },
  hofBadge: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "2px 8px",
    borderRadius: "12px",
    backgroundColor: "#FDF2F2",
    border: "1px solid #F8B4B4",
    color: "#882636",
    fontSize: "12px",
    fontWeight: 700,
    marginLeft: "8px",
    letterSpacing: "0.5px",
    lineHeight: "1.4",
  },
  dropdownWrap: {
    margin: 0,
    position: "relative",
  },
  dropdownButton: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    background: "#F4F5F7",
    color: "#0B0C0C",
    border: "none",
    borderRadius: 8,
    padding: "12px 20px",
    fontSize: 16,
    fontWeight: 500,
    cursor: "pointer",
  },
  dropdownMenu: {
    overflow: "revert",
    minWidth: 220,
    right: 0,
    left: "auto",
    zIndex: 1000,
    marginTop: 8,
    borderRadius: 8,
    boxShadow: "0 4px 16px rgba(0, 0, 0, 0.12)",
    border: "1px solid #E8EAED",
  },

  familyMemberAdded: {
    headLabel: {
      fontWeight: "600",
      fontSize: "15px",
      marginBottom: "12px",
    },
    tableWrapper: {
      marginBottom: "24px",
      overflow: "visible",
    },
    tableCell: {
      padding: "16px 20px",
      fontSize: "14px",
      verticalAlign: "middle",
    },
    nameWrap: {
      display: "flex",
      alignItems: "center",
      gap: "6px",
    },
    nameText: {
      fontWeight: 600,
      color: "#111827",
      fontSize: "14px",
    },
    youBadge: {
      color: "#6B7280",
      fontSize: "12px",
      fontWeight: 500,
    },
    dropdownWrap: {
      minWidth: "160px",
      maxWidth: "220px",
    },
    dropdown: {
      marginBottom: 0,
    },
    dropdownCard: {
      maxHeight: "180px",
      overflowY: "auto",
      zIndex: 9999,
    },
    relationshipText: {
      color: "#374151",
      fontSize: "14px",
      fontWeight: 500,
    },
    radioWrap: {
      display: "inline-flex",
      alignItems: "center",
      cursor: "pointer",
    },
    radioInput: {
      width: "18px",
      height: "18px",
      cursor: "pointer",
      accentColor: "#A82227",
      margin: 0,
    },
    actionDisabled: {
      color: "#9CA3AF",
    },
    actionsWrap: { display: "flex", alignItems: "center", gap: "12px" },
    deleteBtn: {
      background: "none",
      border: "none",
      cursor: "pointer",
      padding: "4px",
      display: "flex",
      alignItems: "center",
    },
    deleteIcon: {
      width: "20px",
      height: "20px",
    },
    deleteIconFill: "#a82227",
    addAnotherSection: {
      marginTop: "8px",
      marginBottom: "28px",
    },
    addBtn: {
      background: "#882636",
      border: "1px solid #882636",
      color: "#FFFFFF",
      borderRadius: "4px",
      padding: "0 20px",
      height: "40px",
      fontSize: "14px",
      fontWeight: "600",
      cursor: "pointer",
      display: "inline-flex",
      alignItems: "center",
      gap: "8px",
      boxSizing: "border-box",
      transition: "all 0.2s ease",
    },
    addBtnHoverBg: "#731f2d",
    addBtnIcon: {
      fontSize: "18px",
      fontWeight: "700",
      lineHeight: 1,
    },
  },

  myFamilyDashboard: {
    pageCard: {
      maxWidth: "355px",
      minHeight: "710px",
      margin: "0 auto",
      padding: "24px 20px 20px",
      boxSizing: "border-box",
    },

    stepText: {
      fontSize: "12px",
      fontWeight: "700",
      color: "#6B7280",
      letterSpacing: "0.4px",
      marginBottom: "14px",
    },

    /* ==============================
       HEADER
    ============================== */

    headerRow: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      gap: "8px",
      marginBottom: "8px",
    },

    pageTitle: {
      fontSize: "24px",
      fontWeight: "700",
      color: "#172033",
      lineHeight: "1.2",
    },

    familyIdBadge: {
      background: "#D9F7E1",
      color: "#087A36",
      borderRadius: "8px",
      padding: "5px 9px",
      fontSize: "11px",
      fontWeight: "700",
      whiteSpace: "nowrap",
    },

    /* ==============================
       FAMILY SUMMARY
    ============================== */

    familySummary: {
      marginBottom: "28px",
    },

    summaryLine: {
      display: "flex",
      alignItems: "baseline",
      gap: "4px",
      flexWrap: "wrap",
      lineHeight: "1.45",
    },

    summaryLabel: {
      fontSize: "15px",
      color: "#65758B",
      fontWeight: "400",
    },

    summaryValue: {
      fontSize: "15px",
      color: "#65758B",
      fontWeight: "400",
    },

    /* ==============================
       REGISTERED MEMBERS HEADER
    ============================== */

    membersHeader: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: "14px",
    },

    membersTitle: {
      fontSize: "11px",
      fontWeight: "700",
      color: "#65758B",
      letterSpacing: "0.2px",
    },

    addMemberLink: {
      border: "none",
      background: "transparent",
      padding: "0",
      color: "#1769E0",
      fontSize: "13px",
      fontWeight: "600",
      textDecoration: "underline",
      cursor: "pointer",
      display: "inline-flex",
      alignItems: "center",
      gap: "5px",
    },

    addMemberPlus: {
      fontSize: "18px",
      fontWeight: "700",
      lineHeight: "1",
      textDecoration: "none",
    },

    /* ==============================
       MEMBER GRID
    ============================== */

    membersGrid: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: "8px 20px",
      marginBottom: "28px",
    },

    memberCard: {
      minHeight: "166px",
      border: "1px solid #DCE3EC",
      borderRadius: "12px",
      background: "#FFFFFF",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "flex-start",
      padding: "16px 10px 14px",
      boxSizing: "border-box",
    },

    /* ==============================
       AVATAR
    ============================== */

    avatar: {
      width: "44px",
      height: "44px",
      borderRadius: "50%",
      background: "#292929",
      color: "#FFFFFF",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      fontSize: "17px",
      fontWeight: "400",
      marginBottom: "12px",
    },

    /* ==============================
       MEMBER DETAILS
    ============================== */

    memberName: {
      fontSize: "14px",
      fontWeight: "700",
      color: "#172033",
      textAlign: "center",
      marginBottom: "5px",
    },

    memberRelationship: {
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "3px",
      color: "#65758B",
      fontSize: "12px",
      textAlign: "center",
      lineHeight: "1.3",
      minHeight: "18px",
    },

    bullet: {
      fontSize: "12px",
      lineHeight: "1",
    },

    youText: {
      color: "#65758B",
    },

    /* ==============================
       VERIFIED BADGE
    ============================== */

    verifiedBadge: {
      marginTop: "10px",
      background: "#D9F7E1",
      color: "#087A36",
      borderRadius: "4px",
      padding: "4px 9px",
      fontSize: "10px",
      fontWeight: "700",
      letterSpacing: "0.1px",
    },

    /* ==============================
       BOTTOM CTA
    ============================== */

  dashboardButton: {
    width: "30%",
    height: "46px",
    border: "none",
    borderRadius: "7px",
    background: "#292929",
    color: "#FFFFFF",
    fontSize: "16px",
    fontWeight: "500",
    cursor: "pointer",
    padding: "0 18px",
    boxSizing: "border-box",
  },
},
propertyDetails: {
  container: {
    width: "100%",
    backgroundColor: "#FFFFFF",
    padding: "16px",
    borderRadius: "8px",
    boxSizing: "border-box",
  },
  emptyState: {
    padding: "24px",
    marginBottom: "16px",
    border: "1px solid #d6d5d5",
    borderRadius: "8px",
    color: "#505A5F",
  },
  list: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    width: "60%",
    maxWidth: "720px",
  },
  summaryCard: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    boxSizing: "border-box",
    width: "100%",
    padding: "12px 14px",
    backgroundColor: "#FFFFFF",
    border: "1px solid #D6D6D6",
    borderRadius: "8px",
    boxShadow: "0 1px 3px rgba(0, 0, 0, 0.16)",
  },
  summaryTitle: {
    fontWeight: "700",
    fontSize: "16px",
  },
  summaryLocation: {
    marginTop: "6px",
  },
  summaryType: {
    marginTop: "8px",
  },
  summaryMeta: {
    marginTop: "6px",
  },
  propertyOwnershipQuestion: {
    marginBottom: "12px",
  },
  ruralBadge: {
    background: "#E7F5EC",
    borderRadius: "12px",
    padding: "3px 8px",
  },
  urbanBadge: {
    background: "#E8F1F8",
    borderRadius: "12px",
    padding: "3px 8px",
  },
  cardActions: {
    display: "flex",
    gap: "8px",
  },
  iconButton: {
    border: "none",
    background: "transparent",
    cursor: "pointer",
  },
  addPropertyButton: {
    marginTop: "16px",
    minHeight: "40px",
    border: "1px solid #0B4F9C",
    borderRadius: "4px",
    background: "#FFFFFF",
    color: "#0B4F9C",
    padding: "8px 16px",
    cursor: "pointer",
  },
  addPropertySticky: {
    position: "sticky",
    top: 0,
    zIndex: 5,
    width: "fit-content",
    padding: "8px 0",
    backgroundColor: "#FFFFFF",
  },
  primaryBadge: {
    display: "inline-block",
    marginLeft: "8px",
    padding: "3px 8px",
    borderRadius: "12px",
    backgroundColor: "#E7F5EC",
    color: "#216E39",
    fontSize: "12px",
    fontWeight: "600",
    verticalAlign: "middle",
  },
  primaryActionButton: {
    padding: "6px 10px",
    border: "1px solid #0B4F9C",
    borderRadius: "4px",
    backgroundColor: "#FFFFFF",
    color: "#0B4F9C",
    fontSize: "12px",
    cursor: "pointer",
  },
  saveNextWrapper: {
    marginTop: "24px",
  },
  drawerOverlay: {
    position: "fixed",
    inset: 0,
    zIndex: 10000,
    background: "rgba(11, 12, 12, 0.35)",
  },
  drawer: {
    position: "absolute",
    right: 0,
    top: "16px",
    bottom: 0,
    width: "min(460px, 92vw)",
    background: "#FFFFFF",
    display: "flex",
    flexDirection: "column",
    borderTopLeftRadius: "8px",
    boxShadow: "-4px 0 16px rgba(0, 0, 0, 0.12)",
  },
  drawerHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "16px 20px",
    borderBottom: "1px solid #d6d5d5",
  },
  closeButton: {
    border: "none",
    background: "transparent",
    fontSize: "28px",
    cursor: "pointer",
  },
  drawerBody: {
    flex: 1,
    overflowY: "auto",
    padding: "20px",
  },
  drawerFooter: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "12px",
    padding: "16px 20px",
    borderTop: "1px solid #d6d5d5",
  },
  drawerCancelButton: {
    width: "100%",
    height: "33px",
    boxSizing: "border-box",
    border: "1px solid #505A5F",
    background: "#FFFFFF",
    color: "#505A5F",
    fontSize: "14px",
    cursor: "pointer",
  },
  cancelButton: {
    minWidth: "100px",
    height: "40px",
    border: "1px solid #505A5F",
    borderRadius: "4px",
    background: "#FFFFFF",
  },
  pageTitle: {
    fontSize: "24px",
    fontWeight: "600",
    marginBottom: "24px",
  },

    actions: {
      display: "flex",
      justifyContent: "flex-end",
      alignItems: "center",
      gap: "12px",
      marginTop: "32px",
      paddingTop: "24px",
      borderTop: "1px solid #e0e0e0",
    }
  },
  urbanPropertyForm: {
    container: {
      width: "100%",
    },

    field: {
      marginBottom: "24px",
    },

    searchInput: {
      width: "100%",
    },

    searchRow: {
      display: "flex",
      alignItems: "flex-start",
      gap: "16px",
    },

    searchSubmitButton: {
      height: "40px",
      padding: "0 16px",
      borderRadius: "4px",
      fontSize: "13px",
      marginTop: 0,
    },

  results: {
    width: "100%",
    marginTop: "16px",
  },

    primaryAddressLabel: {
      marginBottom: "8px",
      fontSize: "14px",
      fontWeight: "600",
    },

    propertyCard: {
      position: "relative",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start",
      gap: "16px",
      padding: "12px 44px 12px 16px",
      marginBottom: "12px",
      backgroundColor: "#F7F7F7",
      border: "1px solid #E8E8E8",
      borderRadius: "8px",
      boxShadow: "none",
    },

    propertyCardWithActions: {
      paddingTop: "48px",
    },

    propertyDetails: {
      display: "grid",
      gridTemplateColumns: "1fr",
      gap: "8px",
    },

    propertyField: {
      fontSize: "12px",
    },

    propertyLabel: {
      color: "#505A5F",
      fontWeight: "600",
    },

    propertyValue: {
      color: "#0B0C0C",
      marginTop: "2px",
      overflowWrap: "anywhere",
    },

    propertyActions: {
      position: "absolute",
      top: "10px",
      right: "12px",
      display: "flex",
      alignItems: "center",
      flexWrap: "wrap",
      justifyContent: "flex-end",
      gap: "8px",
    },

    propertyActionButton: {
      padding: "6px 10px",
      border: "1px solid #b1afb4",
      borderRadius: "16px",
      background: "#FFFFFF",
      color: "#000000",
      fontSize: "12px",
      fontWeight: "600",
      cursor: "pointer",
    },

  defaultPropertyButton: {
    padding: "6px 12px",
    border: "1px solid #2E7D32",
    borderRadius: "16px",
    background: "#E8F5E9",
    color: "#1B5E20",
    fontSize: "12px",
    fontWeight: "700",
    cursor: "default",
  },

    removePropertyButton: {
      borderColor: "#D4353A",
      color: "#D4353A",
    },

  noProperty: {
    marginTop: "16px",
    fontSize: "14px",
  },
  duplicatePropertyError: {
    marginTop: "16px",
    color: "#D4351C",
    fontSize: "14px",
    fontWeight: "600",
  },

    createProperty: {
      marginTop: "20px",
      marginBottom: "10px",
    },

    registerPropertyLink: {
      color: "#D4353A",
      cursor: "pointer",
      fontSize: "14px",
      fontWeight: "600",
      padding: "0 5px",
      textDecoration: "underline"
    },

    createPropertyButton: {
      padding: "8px 0",
      border: "none",
      background: "transparent",
      cursor: "pointer",
      fontSize: "14px",
      fontWeight: "600",
    },
  },
  ruralPropertyForm: {
    container: {
      width: "100%",
    },
    fieldRow: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      columnGap: "24px",
      alignItems: "start",
    },

    field: {
      marginBottom: "20px",
    },

    header: {
      marginBottom: "24px",
    },

    quickRegistration: {
      fontSize: "12px",
      fontWeight: "600",
      marginBottom: "8px",
    },

    title: {
      fontSize: "24px",
      fontWeight: "600",
      marginBottom: "8px",
    },

    description: {
      fontSize: "14px",
      lineHeight: "20px",
      marginBottom: "24px",
    },

    section: {
      marginBottom: "32px",
    },

    sectionTitle: {
      fontSize: "16px",
      fontWeight: "600",
      marginBottom: "20px",
    },

    // field: {
    //   marginBottom: "20px",
    // },

    radioGroup: {
      display: "flex",
      flexDirection: "row",
      alignItems: "center",
      gap: "24px",
      width: "100%",
      flexWrap: "nowrap",
    },
    jointOwnerSection: {
      marginTop: "20px",
      padding: "20px",
      border: "1px solid #E0E0E0",
    },

    jointOwnerTitle: {
      fontSize: "14px",
      fontWeight: "600",
      marginBottom: "20px",
    },

    hint: {
      fontSize: "12px",
      marginTop: "8px",
    },
  },
  aadhaarVerification: {
    consentBox: {
      background: "#F8F9FA",
      padding: "14px 16px",
      borderRadius: "8px",
      marginTop: "16px",
      marginBottom: "8px",
      display: "flex",
      gap: "10px",
      alignItems: "center",
    },
    consentIcon: {
      fontSize: "16px",
    },
    consentText: {
      fontSize: "14px",
      color: "#505A5F",
      lineHeight: "1.4",
    },
    otpSection: {
      marginTop: "24px",
    },
    otpTimerWrap: {
      marginTop: "16px",
      marginBottom: "12px",
    },
    otpTimerText: {
      fontSize: "14px",
      color: "#505A5F",
    },
    otpResendBtn: {
      color: "#F47738",
      cursor: "pointer",
      fontWeight: "600",
      fontSize: "14px",
    },
    otpActionRow: {
      display: "flex",
      alignItems: "center",
      gap: "12px",
      marginTop: "16px",
    },
    editAadhaarBtn: {
      background: "transparent",
      border: "1px solid #C84C0E",
      color: "#C84C0E",
      borderRadius: "4px",
      padding: "0 18px",
      fontSize: "16px",
      fontWeight: "600",
      cursor: "pointer",
      display: "inline-flex",
      alignItems: "center",
      gap: "6px",
      height: "40px",
      boxSizing: "border-box",
    },
    editAadhaarIcon: {
      width: "16px",
      height: "16px",
    },
  },

  verifiedBanner: {
    margin: "16px 0",
    padding: "14px 16px",
    background: "#F0FDF4",
    border: "1px solid #BBF7D0",
    borderRadius: "8px",
    color: "#166534",
    fontWeight: "600",
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },

  // Two-column layout for the library's AddressDetails form, scoped to the address modal.
  addressModalPopup: {
    backgroundColor: "#fff",
    position: "relative",
    width: "min(900px, 95vw)",
    maxHeight: "90vh",
    overflowY: "auto", // safety net for very small screens
    padding: 24,
    borderRadius: 12,
    border: "1px solid #E8EAED",
    boxShadow: "0 8px 24px rgba(0, 0, 0, 0.16)",
  },

  // Documents tab (EditDocuments)
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

  componentBottomMargin: {
    marginBottom: "20px"
  },

  familyCreateTabs: {
    wrapper: {
      display: "flex",
      gap: "8px",
      flexWrap: "wrap",
      marginBottom: "24px",
      borderBottom: "2px solid #E8EAED",
      paddingBottom: "0",
    },
    tab: {
      padding: "10px 20px",
      border: "none",
      borderBottom: "3px solid transparent",
      background: "transparent",
      fontSize: "14px",
      fontWeight: "500",
      color: "#505A5F",
      cursor: "pointer",
      marginBottom: "-2px",
      transition: "all 0.2s ease",
    },
    active: {
      color: "#A82227",
      borderBottom: "3px solid #A82227",
      fontWeight: "700",
    },
    completed: {
      color: "#00703C",
      borderBottom: "3px solid #00703C",
    },
  },
  // Tax ID validate button (IncomeDetails)
  taxIdInputRow: {
    display: "flex",
    gap: "8px",
    alignItems: "flex-start",
  },
  taxIdInputWrap: {
    flex: 1,
  },
  validateBtn: {
    height: "40px",
    padding: "0 16px",
    borderRadius: "4px",
    border: "none",
    background: "var(--app-btn-primary-gradient, linear-gradient(90deg, #6A4A91 0%, #3E285F 100%))",
    color: "#ffffff",
    fontWeight: 600,
    fontSize: "13px",
    cursor: "pointer",
    whiteSpace: "nowrap",
    flexShrink: 0,
    transition: "all 0.2s ease",
  },
  validateBtnSuccess: {
    background: "linear-gradient(90deg, #00703C 0%, #005a30 100%)",
  },
  validateBtnDisabled: {
    opacity: 0.5,
    cursor: "not-allowed",
  },
  validateSuccessMsg: {
    color: "#00703C",
    fontSize: "12px",
    margin: "4px 0 0",
    fontWeight: 500,
  },

  gridStyle: {
    display: "grid",
    gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
    gap: "20px 24px",
    width: "100%",
    boxSizing: "border-box",
    alignItems: "start",
    marginBottom: "20px"
  },

  dropDownHeight: {
    maxHeight: "220px",
    overflowY: "auto",
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

  foundFamilyDetailSection: {
    sectionBoxStyle: {
      marginBottom: "24px",
      padding: "16px 20px",
      background: "#FAFAFA",
      borderRadius: "6px",
      border: "1px solid #E8E8E8",
    },
    sectionTitleStyle: {
      fontSize: "20px",
      fontWeight: "700",
      color: "#0B0C0C",
      marginBottom: "16px",
      paddingBottom: "8px",
      borderBottom: "2px solid #D6D5D4",
      display: "flex",
      alignItems: "center",
      gap: "8px",
    },
    headerWrap: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: "20px",
    },
    actionsWrap: {
      display: "flex",
      alignItems: "center",
      gap: "12px",
    },
    actionBtn: {
      display: "flex",
      alignItems: "center",
      gap: "8px",
      background: "#F4F5F7",
      color: "#0B0C0C",
      border: "none",
      borderRadius: "8px",
      padding: "8px 16px",
      fontSize: "14px",
      fontWeight: "500",
      cursor: "pointer",
    },
  },
  vehicleDetails: {
    listWrapper: {
      marginTop: "24px",
    },
    card: {
      background: "#FAFAFA",
      borderRadius: "6px",
      border: "1px solid #E8E8E8",
      padding: "20px",
      marginBottom: "24px",
    },
    cardHeader: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: "16px",
      paddingBottom: "8px",
      borderBottom: "1px solid #E0E0E0",
    },
    cardTitle: {
      fontSize: "18px",
      fontWeight: "700",
      color: "#0B0C0C",
    },
    deleteBtn: {
      background: "none",
      border: "none",
      cursor: "pointer",
      padding: "4px",
      display: "flex",
      alignItems: "center",
    },
    addVehicleWrapper: {
      marginTop: "16px",
      marginBottom: "24px",
    },
    vehcileMaxNumber:{
      fontSize: "14px", 
      fontWeight: "normal", 
      color: "#fefefe", 
      marginLeft:"5%"
    }
  },
};



/**
 * Two-column layout for the library's AddressDetails form, scoped to the address modal.
 * Kept as a CSS string because selectors (:has, sibling, media query) can't be inline styles.

 * Two-column layout for the library's AddressDetails form, scoped to the address modal.
 * Row-by-Row pairs:
 * Col 1: Address Type   | Col 2: House No.
 * Col 1: Street Name    | Col 2: Address Line 1
 * Col 1: Address Line 2 | Col 2: Landmark
 * Col 1: City           | Col 2: Locality
 * Col 1: Pincode        | Col 2: Address Proof
 */
export const addressModalCss = `
  /* separator below the modal header */
  .address-modal-grid {
    border-top: 1px solid #E8EAED;
    padding-top: 8px;
  }

  /* 2 columns, row-by-row dense placement */
  .address-modal-grid div:has(> .card-label) {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-auto-flow: row dense;
    column-gap: 32px;
    align-content: start;
  }

  /* Odd fields (Address Type, Street Name, Address Line 2, City, Pincode) -> Column 1 */
  .address-modal-grid div:has(> .card-label) > *:nth-child(4n + 1),
  .address-modal-grid div:has(> .card-label) > *:nth-child(4n + 2) {
    grid-column: 1;
  }

  /* Even fields (House No, Address Line 1, Landmark, Locality, Address Proof) -> Column 2 */
  .address-modal-grid div:has(> .card-label) > *:nth-child(4n + 3),
  .address-modal-grid div:has(> .card-label) > *:nth-child(4n + 4) {
    grid-column: 2;
  }

  .address-modal-grid .card-label {
    font-size: 16px;
    margin: 0;
    padding: 12px 0 6px;
    align-self: end;
  }
  .address-modal-grid .card-label + * {
    align-self: start;
    min-width: 0;
  }

  /* rounded inputs and dropdowns */
  .address-modal-grid .employee-card-input,
  .address-modal-grid .select,
  .address-modal-grid .select-wrap {
    border-radius: 8px;
  }

  .address-modal-grid .card {
    box-shadow: none;
    padding: 0;
    margin: 0;
  }

  /* mobile: single column */
  @media (max-width: 640px) {
    .address-modal-grid div:has(> .card-label) {
      grid-template-columns: 1fr;
    }
    .address-modal-grid div:has(> .card-label) > * {
      grid-column: 1 !important;
    }
  }
};
`;

export const propertyDetailsResponsiveCss = `
  @media (max-width: 600px) {
    .profile-property-drawer {
      top: 12px !important;
      right: 12px !important;
      bottom: 12px !important;
      left: 12px !important;
      width: auto !important;
      max-height: calc(100dvh - 24px);
      border-radius: 8px !important;
    }

    .profile-property-drawer-body {
      padding: 16px !important;
    }

    .profile-property-drawer-footer {
      padding: 12px 16px calc(12px + env(safe-area-inset-bottom)) !important;
    }

    .profile-property-drawer-footer .submit-bar,
    .profile-property-drawer-footer .submit-bar-disabled {
      width: 100% !important;
    }

    .profile-property-drawer-footer > button {
      min-height: 40px;
    }

    .profile-property-rural-row {
      grid-template-columns: minmax(0, 1fr) !important;
      column-gap: 0 !important;
    }

    .profile-property-urban-search-row {
      flex-direction: column !important;
      gap: 8px !important;
    }

    .profile-property-urban-search-row > * {
      width: 100% !important;
      box-sizing: border-box !important;
    }

    .profile-property-urban-card {
      padding: 12px !important;
    }

    .profile-property-urban-actions {
      position: static !important;
      justify-content: flex-start !important;
      width: 100%;
      margin-top: 12px;
    }

    .profile-property-urban-actions button {
      min-height: 40px;
    }

    .profile-property-summary-card {
      flex-direction: column !important;
      gap: 12px !important;
    }

    .profile-property-summary-actions {
      width: 100%;
      justify-content: flex-end;
    }

    .profile-property-summary-list {
      width: 100% !important;
    }
  }
`;