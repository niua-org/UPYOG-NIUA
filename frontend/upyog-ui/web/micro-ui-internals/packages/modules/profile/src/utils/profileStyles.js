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
  cardLabel: { width: "100%", fontWeight: 600, color: "#505A5F" },

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
  background: "#F4F5F7",
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
};