import React, { useEffect, useState,Fragment } from "react";
import { useTranslation } from "react-i18next";
import { Loader } from "@upyog/digit-ui-react-components";

// Keep this same for other numbers
const formatNumbers = (amount) => {
  if (amount === null || amount === undefined) return "";

  const num = Number(amount);
  const numStr = num.toString();

  if (numStr.length <= 5) {
    const lastThree = numStr.substring(numStr.length - 3);
    const otherNums = numStr.substring(0, numStr.length - 3);
    const formatted = otherNums.replace(/\B(?=(\d{2})+(?!\d))/g, ",");
    return otherNums ? formatted + "," + lastThree : lastThree;
  } else if (num >= 10000000) {
    const crores = num / 10000000;
    if (crores >= 100) return `${Math.round(crores)} Crores`;
    else if (crores >= 10) return `${crores.toFixed(1)} Crores`;
    else return `${crores.toFixed(2)} Crores`;
  } else if (num >= 100000) {
    const lakhs = num / 100000;
    if (lakhs >= 100) return `${Math.round(lakhs)} Lakhs`;
    else if (lakhs >= 10) return `${lakhs.toFixed(1)} Lakhs`;
    else return `${lakhs.toFixed(2)} Lakhs`;
  }
  return numStr;
};

// ✅ Updated version for full ₹xx,xx,xxx format (no Lakhs/Crores)
const formatIndianCurrency = (amount) => {
  if (amount === null || amount === undefined) return "";
  return `₹${new Intl.NumberFormat("en-IN").format(amount)}`;
};

const ESTDashboard = () => {
  const { t } = useTranslation();
  const [cardData, setCardData] = useState([
    { title: "", count: null, color: "blue" },
    { title: "", count: null, color: "teal" },
    { title: "", count: null, color: "purple" },
    { title: "", count: null, color: "green" },
  ]);

  useEffect(() => {
    const fetchESTDashboardData = async () => {
      try {
        const tenantId = Digit.ULBService.getCurrentUlb
          ? Digit.ULBService.getCurrentUlb().code
          : "tenant-demo";
        const payload = { tenantId, moduleName: "EST" };

        // ✅ DUMMY DATA (Your provided values)
        const mockResponse = {
          estDashboard: {
            totalSpace: 26789,     // ✅ Total Space
            occupiedSpace: 3089,    // ✅ Occupied Space
            pendingRequest: 4098,   // ✅ Pending Request
            totalRevenue: 2330890,  // ✅ ₹23,30,890
          },
        };

        // Try real API first, fallback to mock data
        let response;
        try {
          response = await Digit?.ESTDashboardService?.search(payload);
        } catch (err) {
          console.warn("Using mock data (backend not reachable)");
        }
        response = response || mockResponse;

        if (response && response.estDashboard) {
          setCardData([
            {
              title: t("EST_TOTAL_SPACE"),
              count: response.estDashboard.totalSpace || 0,
              color: "blue",
            },
            {
              title: t("EST_OCCUPIED_SPACE"),
              count: response.estDashboard.occupiedSpace || 0,
              color: "teal",
            },
            {
              title: t("EST_PENDING_REQUEST"),
              count: response.estDashboard.pendingRequest || 0,
              color: "purple",
            },
            {
              title: t("EST_TOTAL_REVENUE"),
              count: response.estDashboard.totalRevenue || 0,
              color: "green",
              isAmount: true,
            },
          ]);
        }
      } catch (error) {
        console.error("Error fetching EST dashboard data:", error);
      }
    };

    fetchESTDashboardData();
  }, [t]);

  return (
    <>
      <div
        style={{
          marginLeft: "42%",
          fontWeight: "bold",
          fontSize: "22px",
          marginBottom: "5px",
        }}
      >
        {t("EST_DASHBOARD")}
      </div>

      <div className="ground-container moduleCardWrapper gridModuleWrapper">
        {cardData.map(({ title, count, color, isAmount }, idx) => (
          <div key={idx} className={`status-card ${color}`}>
            <div className="card-content">
              {count === null ? (
                <Loader />
              ) : (
                <>
                  <span className="count">
                    {isAmount
                      ? formatIndianCurrency(count)
                      : formatNumbers(count)}
                  </span>
                  <span className="title">{title}</span>
                </>
              )}
            </div>
          </div>
        ))}
      </div>
    </>
  );
};

export default ESTDashboard;
