import React, { useState, useEffect } from "react";
import { Header, Loader, TextInput, Dropdown, SubmitBar, CardLabel, Card } from "@upyog/digit-ui-react-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import EstateApplication from "./est-application";

export const ESTMyApplications = () => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const user = Digit.UserService.getUser().info;

  const [searchTerm, setSearchTerm] = useState("");
  const [status, setStatus] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [allData, setAllData] = useState({ Allotments: [] });

  useEffect(() => {
    fetchAllotments();
  }, []);

  const fetchAllotments = async () => {
    setIsLoading(true);
    try {
      const response = await Digit.ESTService.allotmentSearch({
        tenantId,
        filters: { limit: "100", sortOrder: "ASC", sortBy: "createdTime", offset: "0", tenantId }
      });
      setAllData(response || { Allotments: [] });
    } catch (error) {
      console.error("Error fetching allotments:", error);
      setAllData({ Allotments: [] });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = () => {
    // Search logic will be handled in filteredApplications
  };

  const statusOptions = [
    { code: "ACTIVE", value: t("EST_ACTIVE") },
    { code: "PENDING", value: t("EST_PENDING") },
    { code: "EXPIRED", value: t("EST_EXPIRED") },
  ];

  // Filter applications on frontend
  const filteredApplications = allData?.Allotments?.filter(application => {
    // Filter by user's mobile number
    const matchesMobile = application?.mobileNo === user?.mobileNumber;
    
    // Filter by search term (asset number)
    const matchesSearch = !searchTerm.trim() || 
      application?.assetNo?.toLowerCase().includes(searchTerm.toLowerCase());
    
    // Filter by status
    const matchesStatus = !status?.code || application?.status === status?.code;
    
    return matchesMobile && matchesSearch && matchesStatus;
  }) || [];

  if (isLoading) {
    return <Loader />;
  }

  return (
    <React.Fragment>
      <Header>{`${t("EST_MY_APPLICATIONS")} (${filteredApplications.length})`}</Header>
      <Card>
        <div style={{ marginLeft: "16px" }}>
          <div style={{ display: "flex", flexDirection: "row", alignItems: "center", gap: "16px" }}>
            <div style={{ flex: 1 }}>
              <CardLabel>{t("EST_ASSET_NUMBER")}</CardLabel>
              <TextInput
                placeholder={t("Enter Asset Number")}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ width: "100%", padding: "8px" }}
              />
            </div>
            <div style={{ flex: 1 }}>
              <CardLabel>{t("PT_COMMON_TABLE_COL_STATUS_LABEL")}</CardLabel>
              <Dropdown
                selected={status}
                select={setStatus}
                option={statusOptions}
                placeholder={t("Select Status")}
                optionKey="value"
                style={{ width: "100%" }}
                t={t}
              />
            </div>
            <div style={{ marginTop: "30px" }}>
              <SubmitBar label={t("ES_COMMON_SEARCH")} onSubmit={handleSearch} />
              <p className="link" style={{ marginLeft: "30%", marginTop: "10px" }} onClick={() => { setSearchTerm(""); setStatus(""); }}>
                {t("ES_COMMON_CLEAR_ALL")}
              </p>
            </div>
          </div>
        </div>
      </Card>
      <div>
        {filteredApplications.map((application, index) => (
          <EstateApplication key={index} application={application} tenantId={tenantId} buttonLabel={t("EST_SUMMARY")} />
        ))}
        {filteredApplications.length === 0 && <p style={{ marginLeft: "16px", marginTop: "16px" }}>{t("EST_NO_APPLICATION_FOUND_MSG")}</p>}
      </div>
    </React.Fragment>
  );
};

export default ESTMyApplications;